package burlton.dartzee.code.achievements

import burlton.core.code.obj.HashMapList
import burlton.core.code.util.Debug
import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.db.GAME_TYPE_ROUND_THE_CLOCK
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.dartzee.code.utils.ResourceCache.URL_ACHIEVEMENT_HIGHEST_BUST
import burlton.dartzee.code.utils.getLongestStreak
import java.net.URL

class AchievementClockBestStreak: AbstractAchievement()
{
    override val achievementRef = ACHIEVEMENT_REF_CLOCK_BEST_STREAK
    override val name = ""
    override val desc = "Longest streak of hits in Round the Clock"

    override val redThreshold = 1
    override val orangeThreshold = 3
    override val yellowThreshold = 5
    override val greenThreshold = 7
    override val blueThreshold = 9
    override val pinkThreshold = 12
    override val maxValue = 20

    override fun getIconURL(): URL = URL_ACHIEVEMENT_HIGHEST_BUST

    override fun populateForConversion(playerIds: String)
    {
        val sb = StringBuilder()
        sb.append(" SELECT pt.PlayerId, g.RowId AS GameId, pt.RowId AS ParticipantId, drt.Ordinal, drt.Score, drt.Multiplier, drt.StartingScore, drt.DtLastUpdate")
        sb.append(" FROM Game g, Participant pt, Round rnd, Dart drt")
        sb.append(" WHERE g.GameType = $GAME_TYPE_ROUND_THE_CLOCK")
        sb.append(" AND pt.GameId = g.RowId")
        sb.append(" AND rnd.ParticipantId = pt.RowId")
        sb.append(" AND drt.RoundId = rnd.RowId")
        if (!playerIds.isEmpty())
        {
            sb.append(" AND pt.PlayerId IN ($playerIds)")
        }
        sb.append(" ORDER BY pt.RowId, rnd.RoundNumber, drt.Ordinal")

        val hmPlayerIdToDarts = HashMapList<String, Dart>()
        DatabaseUtil.executeQuery(sb).use{ rs ->
            while (rs.next())
            {
                val playerId = rs.getString("PlayerId")
                val gameId = rs.getString("GameId")
                val participantId = rs.getString("ParticipantId")
                val ordinal = rs.getInt("Ordinal")
                val score = rs.getInt("Score")
                val multiplier = rs.getInt("Multiplier")
                val startingScore = rs.getInt("StartingScore")
                val dtThrown = rs.getTimestamp("DtLastUpdate")

                val drt = Dart(score, multiplier)
                drt.startingScore = startingScore
                drt.participantId = participantId
                drt.ordinal = ordinal
                drt.gameId = gameId
                drt.dtThrown = dtThrown

                hmPlayerIdToDarts.putInList(playerId, drt)
            }
        }

        hmPlayerIdToDarts.forEach{ playerId, darts ->
            val streak = getLongestStreak(darts)
            val lastDart = streak.last()

            Debug.append("" + streak)

            AchievementEntity.factoryAndSave(achievementRef, playerId, lastDart.gameId, streak.size, "", lastDart.dtThrown)
        }
    }
}