package burlton.dartzee.code.screen.game

import burlton.core.code.obj.HandyArrayList
import burlton.core.code.obj.HashMapList
import burlton.core.code.util.Debug
import burlton.core.code.util.MathsUtil
import burlton.core.code.util.addUnique
import burlton.core.code.util.flattenBatches
import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.db.ParticipantEntity
import burlton.dartzee.code.utils.DartsColour
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.desktopcore.code.bean.ScrollTable
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.sql.SQLException
import java.util.*
import java.util.stream.IntStream
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * Shows statistics for each player in a particular game.
 * Runs ad-hoc SQL to get the stats, because the full detail isn't readily available in memory (and would be messy to maintain)
 */
abstract class GameStatisticsPanel : JPanel()
{
    protected var playerNamesOrdered = mutableListOf<String>()
    protected var participants: MutableList<ParticipantEntity>? = null
    protected var hmPlayerToDarts = HashMapList<String, MutableList<Dart>>()
    protected var gameParams: String? = null

    private var tm = DefaultTableModel()

    protected val table = ScrollTable()

    protected abstract fun getRankedRowsHighestWins(): MutableList<String>
    protected abstract fun getRankedRowsLowestWins(): MutableList<String>
    protected abstract fun getHistogramRows(): MutableList<String>

    private fun getHistogramRowNumbers(): MutableList<Int>
    {
        val ret = mutableListOf<Int>()
        for (i in 0 until table.rowCount)
        {
            val columnName = table.getValueAt(i, 0)
            if (getHistogramRows().contains(columnName))
            {
                ret.add(i)
            }
        }

        return ret
    }


    init
    {
        layout = BorderLayout(0, 0)
        add(table, BorderLayout.CENTER)

        table.setBorder(EmptyBorder(10, 5, 0, 5))

        val c = UIManager.getColor("Panel.background")
        val c2 = Color(c.red, c.green, c.blue)
        table.setBackgroundProper(c2)

        table.setShowRowCount(false)
    }

    fun showStats(participants: MutableList<ParticipantEntity>)
    {
        this.participants = participants

        hmPlayerToDarts = HashMapList()

        for (participant in participants)
        {
            val playerName = participant.playerName

            //Ensure all the keys are in this map ready for our empty check lower down
            if (!hmPlayerToDarts.containsKey(playerName))
            {
                hmPlayerToDarts[playerName] = mutableListOf()
            }

            val sbSql = StringBuilder()
            sbSql.append(" SELECT d.Score, d.Multiplier, d.StartingScore, d.SegmentType, rnd.RoundNumber")
            sbSql.append(" FROM Dart d, Round rnd")
            sbSql.append(" WHERE rnd.ParticipantId = " + participant.rowId)
            sbSql.append(" AND d.RoundId = rnd.RowId")
            sbSql.append(" ORDER BY rnd.RoundNumber, d.Ordinal")

            try
            {
                DatabaseUtil.executeQuery(sbSql).use { rs ->
                    var dartsForRound = HandyArrayList<Dart>()
                    var currentRoundNumber = 1

                    while (rs.next())
                    {
                        val score = rs.getInt("Score")
                        val multiplier = rs.getInt("Multiplier")
                        val startingScore = rs.getInt("StartingScore")
                        val segmentType = rs.getInt("SegmentType")

                        val d = Dart(score, multiplier)
                        d.startingScore = startingScore
                        d.segmentType = segmentType

                        val roundNumber = rs.getInt("RoundNumber")
                        if (roundNumber > currentRoundNumber)
                        {
                            hmPlayerToDarts.putInList(playerName, dartsForRound)
                            dartsForRound = HandyArrayList()
                            currentRoundNumber = roundNumber
                        }

                        //only needed for golf but doesn't hurt to always set it
                        d.setGolfHole(roundNumber)
                        d.participantId = participant.rowId

                        dartsForRound.add(d)
                    }

                    //Always add the last one, if it's populated
                    if (!dartsForRound.isEmpty())
                    {
                        hmPlayerToDarts.putInList(playerName, dartsForRound)
                    }
                }
            }
            catch (sqle: SQLException)
            {
                Debug.logSqlException("" + sbSql, sqle)
            }

        }

        if (isSufficientData())
        {
            buildTableModel()
        }
    }

    private fun isSufficientData(): Boolean
    {
        val playerNames = hmPlayerToDarts.keysAsVector

        return playerNames.stream().allMatch { p -> !getFlattenedDarts(p).isEmpty() }
    }

    protected fun getRowWidth(): Int
    {
        return playerNamesOrdered.size + 1
    }


    protected fun getAverageGameRow(): Array<Any?>
    {
        val row = arrayOfNulls<Any>(getRowWidth())
        row[0] = "Avg Game"

        for (i in playerNamesOrdered.indices)
        {
            val playerName = playerNamesOrdered[i]

            val playerPts = getFinishedParticipants(playerName)
            if (playerPts.isEmpty())
            {
                row[i + 1] = "N/A"
            } else
            {
                val scores = playerPts.stream().mapToInt { pt -> pt.finalScore }
                val avg = scores.average().asDouble

                row[i + 1] = MathsUtil.round(avg, 2)
            }
        }

        return row
    }

    protected fun buildTableModel()
    {
        tm = DefaultTableModel()
        tm.addColumn("")

        for (pt in participants!!)
        {
            val playerName = pt.playerName
            playerNamesOrdered.addUnique(playerName)
        }

        for (playerName in playerNamesOrdered)
        {
            tm.addColumn(playerName)
        }

        table.setRowHeight(20)
        table.model = tm
        table.disableSorting()

        addRowsToTable()

        //Rendering
        for (i in 0 until getRowWidth())
        {
            table.getColumn(i).cellRenderer = ScorerRenderer()
            table.getColumn(i).headerRenderer = HeaderRenderer()
        }
    }

    protected fun addRow(row: Array<Any?>)
    {
        tm.addRow(row)
    }

    protected fun getFlattenedDarts(playerName: String): MutableList<Dart>
    {
        val rounds = hmPlayerToDarts[playerName]

        rounds ?: return mutableListOf()

        return rounds.flattenBatches()
    }

    protected fun factoryRow(rowName: String): Array<Any?>
    {
        val row = arrayOfNulls<Any>(getRowWidth())
        row[0] = rowName
        return row
    }

    protected fun getBestGameRow(fn: (s: IntStream) -> OptionalInt): Array<Any?>
    {
        val row = arrayOfNulls<Any>(getRowWidth())
        row[0] = "Best Game"

        for (i in playerNamesOrdered.indices)
        {
            val playerName = playerNamesOrdered[i]
            val playerPts = getFinishedParticipants(playerName)

            if (playerPts.isEmpty())
            {
                row[i + 1] = "N/A"
            }
            else
            {
                val scores = playerPts.stream().mapToInt { pt -> pt.finalScore }
                row[i + 1] = fn.invoke(scores).asInt
            }

        }

        return row
    }

    private fun getFinishedParticipants(playerName: String): MutableList<ParticipantEntity>
    {
        return participants!!.filter { pt -> pt.playerName == playerName && pt.finalScore > -1 }.toMutableList()
    }

    protected abstract fun addRowsToTable()

    private inner class HeaderRenderer : JTextPane(), TableCellRenderer
    {
        init
        {
            val doc = this.styledDocument
            val center = SimpleAttributeSet()
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER)
            doc.setParagraphAttributes(0, doc.length, center, false)
        }

        override fun getTableCellRendererComponent(table: JTable,
                                                   value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int,
                                                   column: Int): Component
        {
            text = value as String
            font = Font("Trebuchet MS", Font.BOLD, 15)
            border = getBorder(column)

            setSize(table.columnModel.getColumn(column).width, preferredSize.height)

            if (column == 0)
            {
                background = Color(0, 0, 0, 0)
                isOpaque = false
            }

            return this
        }

        private fun getBorder(column: Int): MatteBorder
        {
            val top = if (column == 0) 0 else 2
            val left = if (column == 0) 0 else 1
            val right = if (column == getRowWidth() - 1) 2 else 1

            return MatteBorder(top, left, 2, right, Color.BLACK)
        }
    }

    private inner class ScorerRenderer : DefaultTableCellRenderer()
    {
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            horizontalAlignment = SwingConstants.CENTER

            if (column == 0)
            {
                font = Font("Trebuchet MS", Font.BOLD, 15)
            }
            else
            {
                font = Font("Trebuchet MS", Font.PLAIN, 15)
            }

            setColours(table, row, column)
            border = getBorder(table!!, row, column)

            return this
        }

        private fun getBorder(table: JTable, row: Int, column: Int): MatteBorder
        {
            val left = if (column == 0) 2 else 1
            val right = if (column == getRowWidth() - 1) 2 else 1

            val bottom = if (row == table.rowCount - 1) 2 else 0

            return MatteBorder(0, left, bottom, right, Color.BLACK)
        }

        private fun setColours(table: JTable?, row: Int, column: Int)
        {
            if (column == 0)
            {
                //Do nothing
                foreground = null
                background = Color.WHITE
                return
            }

            val tm = table!!.model

            val rowName = table.getValueAt(row, 0)
            if (getRankedRowsHighestWins().contains(rowName))
            {
                val pos = getPositionForColour(tm, row, column, true)
                DartsColour.setFgAndBgColoursForPosition(this, pos, Color.WHITE)
            }
            else if (getRankedRowsLowestWins().contains(rowName))
            {
                val pos = getPositionForColour(tm, row, column, false)
                DartsColour.setFgAndBgColoursForPosition(this, pos, Color.WHITE)
            }
            else if (getHistogramRows().contains(rowName))
            {
                val sum = getHistogramSum(tm, column)

                val thisValue = getDoubleAt(tm, row, column)
                val percent = if (sum == 0L) 0f else thisValue.toFloat() / sum

                val bg = Color.getHSBColor(0.5.toFloat(), percent, 1f)

                foreground = null
                background = bg
            }
            else
            {
                foreground = null
                background = Color.WHITE
            }
        }

        private fun getDoubleAt(tm: TableModel, row: Int, col: Int): Double
        {
            val thisValue = tm.getValueAt(row, col)

            if (thisValue == null)
            {
                Debug.append("ROW: $row, COL: $col")
                return -1.0
            }

            return (thisValue as Number).toDouble()
        }

        private fun getPositionForColour(tm: TableModel, row: Int, col: Int, highestWins: Boolean): Int
        {
            if (tm.getValueAt(row, col) is String)
            {
                return -1
            }

            val myScore = getDoubleAt(tm, row, col)

            var myPosition = 1
            for (i in 1 until tm.columnCount)
            {
                if (i == col || tm.getValueAt(row, i) is String)
                {
                    continue
                }

                val theirScore = getDoubleAt(tm, row, i)

                //Compare positivity to the boolean
                val result = java.lang.Double.compare(theirScore, myScore)
                if (result > 0 == highestWins && result != 0)
                {
                    myPosition++
                }
            }

            return myPosition
        }

        private fun getHistogramSum(tm: TableModel, col: Int): Long
        {
            return getHistogramRowNumbers().stream()
                    .mapToLong { row -> (tm.getValueAt(row, col) as Number).toLong() }
                    .sum()
        }
    }
}