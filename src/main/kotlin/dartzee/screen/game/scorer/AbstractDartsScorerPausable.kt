package dartzee.screen.game.scorer

import dartzee.core.util.DateStatics
import dartzee.game.state.AbstractPlayerState
import dartzee.logging.CODE_PLAYER_PAUSED
import dartzee.logging.CODE_PLAYER_UNPAUSED
import dartzee.screen.game.GamePanelPausable
import dartzee.utils.InjectedThings.logger
import dartzee.utils.ResourceCache.ICON_PAUSE
import dartzee.utils.ResourceCache.ICON_RESUME
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton

abstract class AbstractDartsScorerPausable<PlayerState: AbstractPlayerState<PlayerState>>(private val parent: GamePanelPausable<*, *, *>) : AbstractDartsScorer<PlayerState>(), ActionListener
{
    private val btnResume = JButton("")
    private var latestState: PlayerState? = null

    init
    {
        btnResume.preferredSize = Dimension(30, 30)
        panelSouth.add(btnResume, BorderLayout.EAST)
        btnResume.isVisible = false
        btnResume.icon = ICON_RESUME

        btnResume.addActionListener(this)
    }

    fun getPaused() = btnResume.icon === ICON_RESUME && btnResume.isVisible

    override fun stateChanged(state: PlayerState)
    {
        super.stateChanged(state)
        latestState = state
    }

    fun toggleResume()
    {
        if (btnResume.icon === ICON_PAUSE)
        {
            logger.info(CODE_PLAYER_PAUSED, "Paused player $playerId")
            btnResume.icon = ICON_RESUME
        }
        else
        {
            logger.info(CODE_PLAYER_UNPAUSED, "Unpaused player $playerId")
            btnResume.icon = ICON_PAUSE
            updateResultColourForPosition(-1)
        }

        latestState?.let(::stateChanged)
    }

    protected fun finalisePlayerResult(state: PlayerState)
    {
        val dartCount = state.getScoreSoFar()
        lblResult.text = "$dartCount Darts"

        if (state.pt.finishingPosition == -1)
        {
            return
        }

        val playerHasFinished = state.pt.dtFinished != DateStatics.END_OF_TIME
        btnResume.isVisible = !playerHasFinished

        if (getPaused() && !playerHasFinished)
        {
            lblResult.text = "Unfinished"
        }

        if (getPaused() || playerHasFinished)
        {
            updateResultColourForPosition(state.pt.finishingPosition)
        }
    }

    override fun actionPerformed(arg0: ActionEvent)
    {
        if (getPaused())
        {
            parent.unpauseLastPlayer()
        }
        else
        {
            parent.pauseLastPlayer()
        }

        toggleResume()
    }
}
