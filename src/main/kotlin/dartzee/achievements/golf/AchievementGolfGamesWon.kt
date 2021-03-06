package dartzee.achievements.golf

import dartzee.achievements.AchievementType
import dartzee.achievements.AbstractAchievementGamesWon
import dartzee.game.GameType
import dartzee.utils.ResourceCache
import java.net.URL

class AchievementGolfGamesWon : AbstractAchievementGamesWon()
{
    override val achievementType = AchievementType.GOLF_GAMES_WON
    override val gameType = GameType.GOLF
    override val name = "Golf Winner"
    override val desc = "Total number of wins in Golf"

    override fun getIconURL(): URL = ResourceCache.URL_ACHIEVEMENT_GOLF_GAMES_WON
}