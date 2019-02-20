package burlton.dartzee.code.db

import burlton.core.code.obj.HandyArrayList
import burlton.dartzee.code.bean.GameParamFilterPanel
import burlton.dartzee.code.bean.GameParamFilterPanelGolf
import burlton.dartzee.code.bean.GameParamFilterPanelRoundTheClock
import burlton.dartzee.code.bean.GameParamFilterPanelX01
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.desktopcore.code.util.DateStatics
import burlton.desktopcore.code.util.isEndOfTime
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*


const val GAME_TYPE_X01 = 1
const val GAME_TYPE_GOLF = 2
const val GAME_TYPE_ROUND_THE_CLOCK = 3
const val GAME_TYPE_DARTZEE = 4

const val CLOCK_TYPE_STANDARD = "Standard"
const val CLOCK_TYPE_DOUBLES = "Doubles"
const val CLOCK_TYPE_TREBLES = "Trebles"

/**
 * Represents a single game of Darts, e.g. X01 or Dartzee.
 */
class GameEntity : AbstractDartsEntity<GameEntity>()
{
    /**
     * DB fields
     */
    var gameType = -1
    var gameParams = ""
    var dtFinish = DateStatics.END_OF_TIME
    var dartsMatchId: Long = -1
    var matchOrdinal = -1



    /**
     * Helpers
     */
    fun getParticipantCount(): Int
    {
        val sb = StringBuilder()
        sb.append("SELECT COUNT(1) FROM ")
        sb.append(ParticipantEntity().tableName)
        sb.append(" WHERE GameId = ")
        sb.append(rowId)

        return DatabaseUtil.executeQueryAggregate(sb)
    }
    fun isFinished(): Boolean
    {
        return !isEndOfTime(dtFinish)
    }
    fun getTypeDesc(): String
    {
        return getTypeDesc(gameType, gameParams)
    }

    override fun getTableName(): String
    {
        return "Game"
    }

    override fun getCreateTableSqlSpecific(): String
    {
        return ("GameType INT NOT NULL, "
                + "GameParams varchar(255) NOT NULL, "
                + "DtFinish timestamp NOT NULL, "
                + "DartsMatchId INT NOT NULL, "
                + "MatchOrdinal INT NOT NULL")
    }

    @Throws(SQLException::class)
    override fun populateFromResultSet(e: GameEntity, rs: ResultSet)
    {
        e.gameType = rs.getInt("GameType")
        e.gameParams = rs.getString("GameParams")
        e.dtFinish = rs.getTimestamp("DtFinish")
        e.dartsMatchId = rs.getLong("DartsMatchId")
        e.matchOrdinal = rs.getInt("MatchOrdinal")
    }

    @Throws(SQLException::class)
    override fun writeValuesToStatement(statement: PreparedStatement, startIx: Int, emptyStatement: String): String
    {
        var i = startIx
        var statementStr = emptyStatement
        statementStr = writeInt(statement, i++, gameType, statementStr)
        statementStr = writeString(statement, i++, gameParams, statementStr)
        statementStr = writeTimestamp(statement, i++, dtFinish, statementStr)
        statementStr = writeLong(statement, i++, dartsMatchId, statementStr)
        statementStr = writeInt(statement, i, matchOrdinal, statementStr)

        return statementStr
    }

    override fun addListsOfColumnsForIndexes(indexes: ArrayList<ArrayList<String>>)
    {
        val gameTypeIndex = ArrayList<String>()
        gameTypeIndex.add("GameType")
        indexes.add(gameTypeIndex)
    }

    override fun getColumnsAllowedToBeUnset(): ArrayList<String>
    {
        val ret = ArrayList<String>()
        ret.add("DartsMatchId")
        return ret
    }

    override fun getGameId(): Long
    {
        return rowId
    }

    fun retrievePlayersVector(): MutableList<PlayerEntity>
    {
        val ret = mutableListOf<PlayerEntity>()

        val whereSql = "GameId = $rowId ORDER BY Ordinal ASC"
        val participants = ParticipantEntity().retrieveEntities(whereSql)

        participants.forEach{
            ret.add(it.player)
        }

        return ret
    }
}

/**
 * Top-level methods
 */
fun factoryAndSave(gameType: Int, gameParams: String): GameEntity
{
    val gameEntity = GameEntity()
    gameEntity.assignRowId()
    gameEntity.gameType = gameType
    gameEntity.gameParams = gameParams
    gameEntity.saveToDatabase()
    return gameEntity
}

fun factoryAndSave(match: DartsMatchEntity): GameEntity
{
    val gameEntity = GameEntity()
    gameEntity.assignRowId()
    gameEntity.gameType = match.gameType
    gameEntity.gameParams = match.gameParams
    gameEntity.dartsMatchId = match.rowId
    gameEntity.matchOrdinal = match.incrementAndGetCurrentOrdinal()
    gameEntity.saveToDatabase()
    return gameEntity
}

/**
 * Ordered by RowId as well because of a bug with loading where the ordinals could get screwed up.
 */
fun retrieveGamesForMatch(matchId: Long): HandyArrayList<GameEntity>
{
    val sb = StringBuilder()
    sb.append("DartsMatchId = ")
    sb.append(matchId)
    sb.append(" ORDER BY MatchOrdinal, RowId")

    return GameEntity().retrieveEntities(sb.toString())
}

fun getTypeDesc(gameType: Int, gameParams: String): String
{
    return when(gameType)
    {
        GAME_TYPE_X01 -> gameParams
        GAME_TYPE_GOLF -> "Golf - $gameParams holes"
        GAME_TYPE_ROUND_THE_CLOCK -> "Round the Clock - $gameParams"
        GAME_TYPE_DARTZEE -> "Dartzee"
        else -> ""
    }
}

fun getTypeDesc(gameType: Int): String
{
    return when (gameType)
    {
        GAME_TYPE_X01 -> "X01"
        GAME_TYPE_GOLF -> "Golf"
        GAME_TYPE_ROUND_THE_CLOCK -> "Round the Clock"
        GAME_TYPE_DARTZEE -> "Dartzee"
        else -> "<Game Type>"
    }
}

fun getFilterPanel(gameType: Int): GameParamFilterPanel?
{
    return when (gameType)
    {
        GAME_TYPE_X01 -> GameParamFilterPanelX01()
        GAME_TYPE_GOLF -> GameParamFilterPanelGolf()
        GAME_TYPE_ROUND_THE_CLOCK -> GameParamFilterPanelRoundTheClock()
        else -> null
    }

}

fun getAllGameTypes(): MutableList<Int>
{
    return mutableListOf(GAME_TYPE_X01, GAME_TYPE_GOLF, GAME_TYPE_ROUND_THE_CLOCK, GAME_TYPE_DARTZEE)
}