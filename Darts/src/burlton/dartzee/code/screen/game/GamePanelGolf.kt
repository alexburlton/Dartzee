package burlton.dartzee.code.screen.game

import burlton.core.code.obj.HashMapList
import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.achievements.ACHIEVEMENT_REF_GOLF_POINTS_RISKED
import burlton.dartzee.code.ai.AbstractDartsModel
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.db.DartEntity
import burlton.desktopcore.code.util.getSqlDateNow

class GamePanelGolf(parent: DartsGameScreen) : DartsGamePanel<DartsScorerGolf>(parent)
{
    //Number of rounds - 9 holes or 18?
    private var numberOfRounds = -1

    private fun getScoreForMostRecentDart() : Int
    {
        val lastDart = dartsThrown.last()

        val targetHole = currentRound.roundNumber
        return lastDart.getGolfScore(targetHole)
    }

    override fun initImpl(gameParams: String)
    {
        //The params tell us how many holes
        numberOfRounds = Integer.parseInt(gameParams)
    }

    override fun doAiTurn(model: AbstractDartsModel)
    {
        val targetHole = currentRound.roundNumber
        val dartNo = dartsThrown.size + 1
        model.throwGolfDart(targetHole, dartNo, dartboard)
    }

    override fun loadDartsForParticipant(playerNumber: Int, hmRoundToDarts: HashMapList<Int, Dart>, lastRound: Int)
    {
        val scorer = hmPlayerNumberToDartsScorer[playerNumber]
        for (i in 1..lastRound)
        {
            val darts = hmRoundToDarts[i]
            scorer?.addDarts(darts)
        }
    }

    override fun updateVariablesForNewRound() {}
    override fun resetRoundVariables() {}
    override fun updateVariablesForDartThrown(dart: Dart) {}

    override fun shouldStopAfterDartThrown(): Boolean
    {
        if (dartsThrown.size == 3)
        {
            return true
        }

        val score = getScoreForMostRecentDart()
        if (activeScorer.getHuman())
        {
            return score == 1
        }
        else
        {
            val noDarts = dartsThrown.size

            val model = currentPlayerStrategy
            val stopThreshold = model!!.getStopThresholdForDartNo(noDarts)

            return score <= stopThreshold
        }
    }

    override fun saveDartsAndProceed()
    {
        val roundId = currentRound.rowId
        for (i in dartsThrown.indices)
        {
            val dart = dartsThrown[i]
            DartEntity.factoryAndSave(dart, roundId, i+1, -1)
        }

        activeScorer.finaliseRoundScore()

        unlockAchievements()

        if (currentRound.roundNumber == numberOfRounds)
        {
            handlePlayerFinish()
        }

        currentPlayerNumber = getNextPlayerNumber(currentPlayerNumber)
        if (activeCount > 0)
        {
            nextTurn()
        }
        else
        {
            finishGame()
        }
    }

    private fun unlockAchievements()
    {
        var pointsRisked = 0
        dartsThrown.forEach{
            val score = it.getGolfScore(currentRound.roundNumber)
            if (score < 5
              && !(dartsThrown.last() === it))
            {
                pointsRisked += 5 - score
            }
        }

        if (pointsRisked > 0)
        {
            AchievementEntity.incrementAchievement(ACHIEVEMENT_REF_GOLF_POINTS_RISKED, currentPlayerId, gameEntity.rowId, pointsRisked)
        }
    }


    override fun getFinishingPositionFromPlayersRemaining(): Int
    {
        //Finishing positions are determined at the end
        return -1
    }

    private fun finishGame()
    {
        //Get the participants sorted by score so we can assign finishing positions
        val participants = hmPlayerNumberToParticipant.values.sortedBy{it.finalScore}

        var previousScore = Integer.MAX_VALUE
        var finishPos = 1
        for (i in participants.indices)
        {
            val pt = participants[i]
            val ptScore = pt.finalScore
            if (ptScore > previousScore)
            {
                finishPos++
            }

            pt.finishingPosition = finishPos
            pt.saveToDatabase()

            previousScore = pt.finalScore
        }

        updateScorersWithFinishingPositions()

        gameEntity.dtFinish = getSqlDateNow()
        gameEntity.saveToDatabase()

        parentWindow.startNextGameIfNecessary()

        allPlayersFinished()
    }

    override fun factoryScorer(): DartsScorerGolf
    {
        return DartsScorerGolf()
    }

    override fun shouldAIStop(): Boolean
    {
        return false
    }

    override fun doMissAnimation()
    {
        dartboard.doGolfMiss()
    }

    override fun factoryStatsPanel(): GameStatisticsPanel
    {
        return GameStatisticsPanelGolf()
    }
}