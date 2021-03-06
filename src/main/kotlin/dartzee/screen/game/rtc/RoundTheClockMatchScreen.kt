package dartzee.screen.game.rtc

import dartzee.db.DartsMatchEntity
import dartzee.db.GameEntity
import dartzee.db.PlayerEntity
import dartzee.game.state.ClockPlayerState
import dartzee.screen.game.AbstractDartsGameScreen
import dartzee.screen.game.DartsMatchScreen
import dartzee.screen.game.MatchSummaryPanel

class RoundTheClockMatchScreen(match: DartsMatchEntity, players: List<PlayerEntity>):
    DartsMatchScreen<ClockPlayerState>(MatchSummaryPanel(match, MatchStatisticsPanelRoundTheClock(match.gameParams)), match, players)
{
    override fun factoryGamePanel(parent: AbstractDartsGameScreen, game: GameEntity) = GamePanelRoundTheClock(parent, game, match.getPlayerCount())
}