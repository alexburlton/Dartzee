package dartzee.`object`

import dartzee.core.obj.HashMapList
import dartzee.utils.getColourForPointAndSegment
import java.awt.Point

/**
 * Stateful version of DartboardSegment, with all its points and edge points
 */
class StatefulSegment(val type: SegmentType, val score: Int)
{
    //The Points this segment contains
    val points = mutableSetOf<Point>()
    val edgePoints = mutableSetOf<Point>()

    //For tracking edge points
    private val hmXCoordToPoints = HashMapList<Int, Point>()
    private val hmYCoordToPoints = HashMapList<Int, Point>()

    fun toDataSegment() = DartboardSegment(type, score)

    /**
     * Helpers
     */
    fun isMiss() = type == SegmentType.MISS
    fun isDoubleExcludingBull() = type == SegmentType.DOUBLE && score != 25
    fun getTotal(): Int = score * type.getMultiplier()

    fun addPoint(pt: Point)
    {
        points.add(pt)

        hmXCoordToPoints.putInList(pt.x, pt)
        hmYCoordToPoints.putInList(pt.y, pt)
    }

    fun getColorMap(colourWrapper: ColourWrapper?) = points.map { pt -> pt to getColourForPointAndSegment(pt, this, colourWrapper) }
    fun containsPoint(point: Point) = points.contains(point)

    override fun toString() = "$score ($type)"

    fun isEdgePoint(pt: Point?): Boolean
    {
        pt ?: return false
        return edgePoints.contains(pt)
    }

    fun computeEdgePoints()
    {
        val yMins: List<Point> = hmXCoordToPoints.values.map { points -> points.minBy { it.y }!! }
        val yMaxes: List<Point> = hmXCoordToPoints.values.map { points -> points.maxBy { it.y }!! }
        val xMins: List<Point> = hmYCoordToPoints.values.map { points -> points.minBy { it.x }!! }
        val xMaxes: List<Point> = hmYCoordToPoints.values.map { points -> points.maxBy { it.x }!! }
        edgePoints.addAll(yMins + yMaxes + xMins + xMaxes)

        hmXCoordToPoints.clear()
        hmYCoordToPoints.clear()
    }
}

