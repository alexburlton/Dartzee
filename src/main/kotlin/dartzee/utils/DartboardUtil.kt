package dartzee.utils

import dartzee.`object`.*
import dartzee.screen.Dartboard
import java.awt.Color
import java.awt.Point

/**
 * Utilities for the Dartboard object.
 */
private val numberOrder = mutableListOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5, 20)

val hmScoreToOrdinal = initialiseOrdinalHashMap()
private var colourWrapperFromPrefs: ColourWrapper? = null

private const val RATIO_INNER_BULL = 0.038
private const val RATIO_OUTER_BULL = 0.094
private const val LOWER_BOUND_TRIPLE_RATIO = 0.582
private const val UPPER_BOUND_TRIPLE_RATIO = 0.629
private const val LOWER_BOUND_DOUBLE_RATIO = 0.953

private const val UPPER_BOUND_DOUBLE_RATIO = 1.0
const val UPPER_BOUND_OUTSIDE_BOARD_RATIO = 1.3

fun getDartForSegment(pt: Point, segment: DartboardSegment): Dart
{
    val score = segment.score
    val multiplier = segment.getMultiplier()
    return Dart(score, multiplier, pt, segment.type)
}

fun getAdjacentNumbers(number: Int): MutableList<Int>
{
    if (number == 20)
    {
        return mutableListOf(1, 5)
    }

    val ix = numberOrder.indexOf(number)
    return mutableListOf(numberOrder[ix-1], numberOrder[ix+1])
}

fun factorySegmentForPoint(dartPt: Point, centerPt: Point, diameter: Double): StatefulSegment?
{
    val radius = getDistance(dartPt, centerPt)
    val ratio = 2 * radius / diameter

    if (ratio < RATIO_INNER_BULL)
    {
        return StatefulSegment(SegmentType.DOUBLE, 25)
    }
    else if (ratio < RATIO_OUTER_BULL)
    {
        return StatefulSegment(SegmentType.OUTER_SINGLE, 25)
    }

    //We've not hit the bullseye, so do other calculations to work out score/multiplier
    val angle = getAngleForPoint(dartPt, centerPt)
    val score = getScoreForAngle(angle)
    val type = calculateTypeForRatioNonBullseye(ratio)

    if (type == SegmentType.MISSED_BOARD)
    {
        return null
    }

    return StatefulSegment(type, score)
}

/**
 * 1) Calculate the radius from the center to our point
 * 2) Using the diameter, work out whether this makes us a miss, single, double or treble
 */
private fun calculateTypeForRatioNonBullseye(ratioToDiameter: Double) =
    when
    {
        ratioToDiameter < LOWER_BOUND_TRIPLE_RATIO -> SegmentType.INNER_SINGLE
        ratioToDiameter < UPPER_BOUND_TRIPLE_RATIO -> SegmentType.TREBLE
        ratioToDiameter < LOWER_BOUND_DOUBLE_RATIO -> SegmentType.OUTER_SINGLE
        ratioToDiameter < UPPER_BOUND_DOUBLE_RATIO -> SegmentType.DOUBLE
        ratioToDiameter < UPPER_BOUND_OUTSIDE_BOARD_RATIO -> SegmentType.MISS
        else -> SegmentType.MISSED_BOARD
    }

private fun getScoreForAngle(angle: Double): Int
{
    var checkValue = 9
    var index = 0
    while (angle > checkValue)
    {
        index++
        checkValue += 18
    }

    return numberOrder[index]
}

data class AimPoint(val centerPoint: Point, val radius: Double, val angle: Int, val ratio: Double)
{
    val point = translatePoint(centerPoint, radius * ratio, angle.toDouble())
}
fun getPotentialAimPoints(centerPt: Point, diameter: Double): Set<AimPoint>
{
    val radius = diameter / 2

    val points = mutableSetOf<AimPoint>()
    for (angle in 0 until 360 step 9)
    {
        points.add(AimPoint(centerPt, radius, angle, (RATIO_OUTER_BULL + LOWER_BOUND_TRIPLE_RATIO)/2))
        points.add(AimPoint(centerPt, radius, angle, (LOWER_BOUND_TRIPLE_RATIO + UPPER_BOUND_TRIPLE_RATIO)/2))
        points.add(AimPoint(centerPt, radius, angle, (UPPER_BOUND_TRIPLE_RATIO + LOWER_BOUND_DOUBLE_RATIO)/2))
        points.add(AimPoint(centerPt, radius, angle, (LOWER_BOUND_DOUBLE_RATIO + UPPER_BOUND_DOUBLE_RATIO)/2))
    }

    points.add(AimPoint(centerPt, radius, 0, 0.0))
    return points.toSet()
}

fun getColourForPointAndSegment(pt: Point, segment: StatefulSegment, colourWrapper: ColourWrapper?): Color
{
    val colourWrapperToUse = colourWrapper ?: getColourWrapperFromPrefs()

    val edgeColour = colourWrapperToUse.edgeColour
    if (edgeColour != null
            && !segment.isMiss()
            && segment.isEdgePoint(pt))
    {
        return edgeColour
    }

    return getColourForSegment(segment.toDataSegment(), colourWrapperToUse)
}

fun getColourForSegment(segment: DartboardSegment, colourWrapper: ColourWrapper?): Color
{
    val colourWrapperToUse = colourWrapper ?: getColourWrapperFromPrefs()
    return getColourFromHashMap(segment, colourWrapperToUse)
}

fun getColourFromHashMap(segment: DartboardSegment, colourWrapper: ColourWrapper): Color
{
    val type = segment.type
    if (type == SegmentType.MISS)
    {
        return colourWrapper.outerDartboardColour
    }

    if (type == SegmentType.MISSED_BOARD)
    {
        return colourWrapper.missedBoardColour
    }

    val score = segment.score
    val multiplier = segment.getMultiplier()

    if (score == 25)
    {
        return colourWrapper.getBullColour(multiplier)
    }

    return colourWrapper.getColour(multiplier, score)
}

private fun getColourWrapperFromPrefs(): ColourWrapper
{
    if (colourWrapperFromPrefs != null)
    {
        return colourWrapperFromPrefs!!
    }

    val evenSingleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_EVEN_SINGLE_COLOUR)
    val evenDoubleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_EVEN_DOUBLE_COLOUR)
    val evenTrebleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_EVEN_TREBLE_COLOUR)
    val oddSingleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_ODD_SINGLE_COLOUR)
    val oddDoubleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_ODD_DOUBLE_COLOUR)
    val oddTrebleStr = PreferenceUtil.getStringValue(PREFERENCES_STRING_ODD_TREBLE_COLOUR)

    val evenSingle = DartsColour.getColorFromPrefStr(evenSingleStr)
    val evenDouble = DartsColour.getColorFromPrefStr(evenDoubleStr)
    val evenTreble = DartsColour.getColorFromPrefStr(evenTrebleStr)

    val oddSingle = DartsColour.getColorFromPrefStr(oddSingleStr)
    val oddDouble = DartsColour.getColorFromPrefStr(oddDoubleStr)
    val oddTreble = DartsColour.getColorFromPrefStr(oddTrebleStr)

    colourWrapperFromPrefs = ColourWrapper(evenSingle, evenDouble, evenTreble,
            oddSingle, oddDouble, oddTreble, evenDouble, oddDouble)

    return colourWrapperFromPrefs!!
}

private fun initialiseOrdinalHashMap() : MutableMap<Int, Boolean>
{
    val ret = mutableMapOf<Int, Boolean>()

    for (i in 0 until numberOrder.size - 1)
    {
        val even = i and 1 == 0
        ret[numberOrder[i]] = even
    }

    return ret
}

fun resetCachedDartboardValues()
{
    colourWrapperFromPrefs = null

    Dartboard.appearancePreferenceChanged()
}

fun getAllPossibleSegments(): List<DartboardSegment>
{
    val segments = mutableListOf<DartboardSegment>()
    for (i in 1..20)
    {
        segments.add(DartboardSegment(SegmentType.DOUBLE, i))
        segments.add(DartboardSegment(SegmentType.TREBLE, i))
        segments.add(DartboardSegment(SegmentType.OUTER_SINGLE, i))
        segments.add(DartboardSegment(SegmentType.INNER_SINGLE, i))
        segments.add(DartboardSegment(SegmentType.MISS, i))
        segments.add(DartboardSegment(SegmentType.MISSED_BOARD, i))
    }

    segments.add(DartboardSegment(SegmentType.OUTER_SINGLE, 25))
    segments.add(DartboardSegment(SegmentType.DOUBLE, 25))

    return segments.toList()
}