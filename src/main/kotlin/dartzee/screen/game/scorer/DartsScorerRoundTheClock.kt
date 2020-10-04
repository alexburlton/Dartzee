 package dartzee.screen.game.scorer

import dartzee.`object`.DartNotThrown
import dartzee.game.ClockType
import dartzee.game.state.ClockPlayerState
import dartzee.screen.game.GamePanelPausable
import java.awt.BorderLayout

 class DartsScorerRoundTheClock(parent: GamePanelPausable<*, *>, private val clockType: ClockType) : AbstractDartsScorerPausable<ClockPlayerState>(parent)
{
    private val tableRemaining = RoundTheClockScorecard()

    override fun getNumberOfColumns() = 4

    override fun getNumberOfColumnsForAddingNewDart() = getNumberOfColumns()

    override fun initImpl()
    {
        for (i in 0..BONUS_COLUMN)
        {
            tableScores.getColumn(i).cellRenderer = RoundTheClockDartRenderer(clockType)
        }

        panelCenter.add(tableRemaining, BorderLayout.NORTH)
    }

    override fun stateChangedImpl(state: ClockPlayerState)
    {
        tableRemaining.stateChanged(state)

        state.completedRounds.forEach { round ->
            addDartRound(round)

            if (round.size < 4)
            {
                disableBrucey()
            }
        }

        val currentRound = state.currentRound
        if (currentRound.isNotEmpty())
        {
            addDartRound(currentRound)

            if (!state.onTrackForBrucey() && currentRound.size < 4)
            {
                disableBrucey()
            }
        }

        finalisePlayerResult(state)
    }

    private fun disableBrucey()
    {
        val row = model.rowCount - 1
        model.setValueAt(DartNotThrown(), row, BONUS_COLUMN)
    }

    companion object
    {
        private const val BONUS_COLUMN = 3
    }
}
