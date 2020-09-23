package dartzee.helper

import dartzee.`object`.Dart
import dartzee.`object`.SegmentType
import dartzee.db.ParticipantEntity
import dartzee.db.PlayerEntity
import dartzee.game.ClockType
import dartzee.game.state.ClockPlayerState
import dartzee.game.state.GolfPlayerState
import dartzee.game.state.X01PlayerState
import dartzee.utils.isBust
import java.awt.Point

fun factoryClockHit(score: Int, multiplier: Int = 1): Dart
{
    val dart = Dart(score, multiplier)
    dart.startingScore = score
    return dart
}

fun makeDart(score: Int = 20,
             multiplier: Int = 1,
             segmentType: SegmentType = getSegmentTypeForMultiplier(multiplier),
             pt: Point = Point(0, 0),
             startingScore: Int = -1,
             golfHole: Int = -1): Dart
{
    val dart = Dart(score, multiplier, pt, segmentType)
    dart.startingScore = startingScore
    dart.roundNumber = golfHole
    return dart
}

fun makeGolfRound(golfHole: Int, darts: List<Dart>): List<Dart>
{
    darts.forEach { it.roundNumber = golfHole }
    return darts
}

fun makeX01Rounds(startingScore: Int = 501, vararg darts: List<Dart>): List<List<Dart>>
{
    var currentScore = startingScore
    darts.forEach {
        var roundScore = currentScore
        it.forEach { dart ->
            dart.startingScore = roundScore
            roundScore -= dart.getTotal()
        }

        val lastDartForRound = it.last()
        if (!isBust(lastDartForRound))
        {
            currentScore = roundScore
        }
    }

    return darts.toList()
}
fun makeX01Rounds(startingScore: Int = 501, vararg darts: Dart): List<List<Dart>>
{
    var currentTotal = startingScore
    darts.forEach {
        it.startingScore = currentTotal
        currentTotal -= it.getTotal()
    }

    return darts.toList().chunked(3)
}

fun makeClockPlayerStateWithRounds(clockType: ClockType = ClockType.Standard,
                                   player: PlayerEntity = insertPlayer(),
                                   participant: ParticipantEntity = insertParticipant(playerId = player.rowId),
                                   completedRounds: List<List<Dart>> = emptyList()): ClockPlayerState
{
    completedRounds.flatten().forEach { it.participantId = participant.rowId }
    return ClockPlayerState(clockType, participant, completedRounds.toMutableList())
}

fun makeX01PlayerState(startingScore: Int = 501,
                       player: PlayerEntity = insertPlayer(),
                       participant: ParticipantEntity = insertParticipant(playerId = player.rowId),
                       dartsThrown: List<Dart> = listOf(makeDart())): X01PlayerState
{
    return X01PlayerState(startingScore, participant, mutableListOf(dartsThrown))
}

fun makeX01PlayerStateWithRounds(startingScore: Int = 501,
                                 player: PlayerEntity = insertPlayer(),
                                 participant: ParticipantEntity = insertParticipant(playerId = player.rowId),
                                 dartsThrown: List<List<Dart>> = emptyList()): X01PlayerState
{
    dartsThrown.flatten().forEach { it.participantId = participant.rowId }
    return X01PlayerState(startingScore, participant, dartsThrown.toMutableList())
}

fun makeGolfPlayerState(player: PlayerEntity = insertPlayer(),
                       participant: ParticipantEntity = insertParticipant(playerId = player.rowId),
                       dartsThrown: List<Dart> = emptyList()): GolfPlayerState
{
    return GolfPlayerState(participant, mutableListOf(), dartsThrown.toMutableList())
}

fun makeGolfPlayerStateWithRounds(player: PlayerEntity = insertPlayer(),
                                 participant: ParticipantEntity = insertParticipant(playerId = player.rowId),
                                 dartsThrown: List<List<Dart>> = emptyList()): GolfPlayerState
{
    dartsThrown.flatten().forEach { it.participantId = participant.rowId }
    return GolfPlayerState(participant, dartsThrown.toMutableList())
}