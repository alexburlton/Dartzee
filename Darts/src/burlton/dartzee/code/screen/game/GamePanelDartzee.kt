package burlton.dartzee.code.screen.game

import burlton.core.code.obj.HashMapList
import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.`object`.DartboardSegment
import burlton.dartzee.code.ai.AbstractDartsModel
import burlton.dartzee.code.dartzee.DartzeeRoundResult
import burlton.dartzee.code.db.DartzeeRoundResultEntity
import burlton.dartzee.code.db.DartzeeRuleEntity
import burlton.dartzee.code.db.GameEntity
import burlton.dartzee.code.screen.dartzee.DartboardRuleVerifier
import burlton.dartzee.code.screen.dartzee.DartzeeRuleSummaryPanel
import burlton.dartzee.code.screen.dartzee.IDartzeeCarouselListener
import burlton.dartzee.code.screen.game.scorer.DartsScorerDartzee
import burlton.dartzee.code.utils.factoryHighScoreResult
import burlton.dartzee.code.utils.getAllPossibleSegments
import java.awt.BorderLayout

/**
 * TODO list
 *  - Implement AI strategy
 *  - Implement in-game stats
 */
class GamePanelDartzee(parent: AbstractDartsGameScreen, game: GameEntity) :
        GamePanelFixedLength<DartsScorerDartzee, DartboardRuleVerifier>(parent, game),
        IDartzeeCarouselListener
{
    private val dtos = DartzeeRuleEntity().retrieveForGame(game.rowId).map { it.toDto() }
    override val totalRounds = dtos.size + 1

    private val summaryPanel = DartzeeRuleSummaryPanel(this, dtos)

    //Transient things
    private var lastRoundScore = -1
    private val hmPlayerNumberToRoundResults = HashMapList<Int, DartzeeRoundResultEntity>()

    init
    {
        add(summaryPanel, BorderLayout.NORTH)
    }

    override fun factoryDartboard() = DartboardRuleVerifier()

    override fun doAiTurn(model: AbstractDartsModel)
    {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setGameReadOnly() {
        super.setGameReadOnly()

        summaryPanel.gameFinished()
        scorerSelected(scorersOrdered.first())
    }

    override fun loadDartsForParticipant(playerNumber: Int, hmRoundToDarts: HashMapList<Int, Dart>, totalRounds: Int)
    {
        val pt = hmPlayerNumberToParticipant[playerNumber]!!

        val roundResults = DartzeeRoundResultEntity().retrieveEntities("PlayerId = '${pt.playerId}' AND ParticipantId = '${pt.rowId}'")
        hmPlayerNumberToRoundResults[playerNumber] = roundResults

        val scorer = hmPlayerNumberToDartsScorer[playerNumber]!!
        for (i in 1..totalRounds)
        {
            val darts = hmRoundToDarts[i]!!
            darts.forEach { scorer.addDart(it) }

            if (i == 1)
            {
                val result = factoryHighScoreResult(darts)
                scorer.setResult(result, result.score)
            }
            else
            {
                val result = roundResults.find { it.roundNumber == i }!!
                scorer.setResult(result.toDto(), result.score)
            }
        }
    }

    override fun updateVariablesForNewRound()
    {
        lastRoundScore = activeScorer.getTotalScore()
    }

    override fun resetRoundVariables() {}

    override fun updateVariablesForDartThrown(dart: Dart)
    {
        updateCarouselAndDartboard()
    }

    override fun shouldStopAfterDartThrown(): Boolean
    {
        val validSegments = summaryPanel.getValidSegments()
        return dartsThrown.size == 3 || validSegments.isEmpty()
    }

    override fun shouldAIStop() = false

    override fun readyForThrow()
    {
        super.readyForThrow()

        updateCarouselAndDartboard()

        if (currentRoundNumber > 1)
        {
            btnConfirm.isVisible = false
        }
    }

    private fun updateCarouselAndDartboard()
    {
        updateCarousel()
        dartboard.refreshValidSegments(summaryPanel.getValidSegments())
    }
    private fun updateCarousel()
    {
        val ruleResults = hmPlayerNumberToRoundResults.getOrDefault(currentPlayerNumber, mutableListOf())
        summaryPanel.update(ruleResults, dartsThrown, lastRoundScore, currentRoundNumber)
    }

    /**
     * Hook called from the confirm button, which is only available for the initial high score round
     */
    override fun saveDartsAndProceed()
    {
        completeRound(factoryHighScoreResult(dartsThrown))
    }

    private fun completeRound(result: DartzeeRoundResult)
    {
        val pt = hmPlayerNumberToParticipant[currentPlayerNumber]!!

        activeScorer.setResult(result, result.score)
        if (currentRoundNumber > 1)
        {
            val entity = DartzeeRoundResultEntity.factoryAndSave(result, pt, currentRoundNumber)
            hmPlayerNumberToRoundResults.putInList(currentPlayerNumber, entity)
        }

        disableInputButtons()
        dartboard.clearDarts()

        saveDartsToDatabase()

        finishRound()

        if (gameEntity.isFinished())
        {
            gameFinished()
        }
    }

    private fun gameFinished()
    {
        summaryPanel.gameFinished()
        dartboard.refreshValidSegments(getAllPossibleSegments())

        updateCarousel()
    }

    override fun factoryStatsPanel(): GameStatisticsPanel? = null
    override fun factoryScorer() = DartsScorerDartzee(this)

    override fun hoverChanged(validSegments: List<DartboardSegment>)
    {
        if (dartsThrown.size < 3)
        {
            dartboard.refreshValidSegments(validSegments)
        }
    }

    override fun tilePressed(dartzeeRoundResult: DartzeeRoundResult)
    {
        completeRound(dartzeeRoundResult)
    }

    fun scorerSelected(scorer: DartsScorerDartzee)
    {
        scorersOrdered.forEach { it.setSelected(false) }
        scorer.setSelected(true)

        currentPlayerNumber = hmPlayerNumberToDartsScorer.filter { it.value == scorer }.keys.first()
        updateCarousel()
    }
}