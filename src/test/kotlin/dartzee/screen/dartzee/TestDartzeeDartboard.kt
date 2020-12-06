package dartzee.screen.dartzee

import com.github.alexburlton.swingtest.shouldMatchImage
import dartzee.*
import dartzee.`object`.DEFAULT_COLOUR_WRAPPER
import dartzee.`object`.GREY_COLOUR_WRAPPER
import dartzee.`object`.SegmentType
import dartzee.helper.AbstractTest
import dartzee.helper.makeSegmentStatus
import dartzee.screen.Dartboard
import dartzee.utils.DartsColour
import dartzee.utils.getAllPossibleSegments
import io.kotlintest.shouldBe
import org.junit.Test
import java.awt.Color
import java.awt.Point

class TestDartzeeDartboard: AbstractTest()
{
    @Test
    fun `Should leave scoring segments the original colour`()
    {
        val dartboard = DartzeeDartboard(150, 150)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)

        val scoringSegments = listOf(trebleNineteen, bullseye)
        dartboard.refreshValidSegments(makeSegmentStatus(scoringSegments))

        val t19Pt = dartboard.getNonEdgePointForSegment(19, SegmentType.TREBLE)
        dartboard.getColor(t19Pt) shouldBe Color.GREEN

        val bullPt = dartboard.getNonEdgePointForSegment(25, SegmentType.DOUBLE)
        dartboard.getColor(bullPt) shouldBe Color.RED
    }

    @Test
    fun `Should colour segments that are only valid in grayscale`()
    {
        val dartboard = DartzeeDartboard(150, 150)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)

        val validSegments = listOf(trebleNineteen, bullseye)
        dartboard.refreshValidSegments(makeSegmentStatus(scoringSegments = emptyList(), validSegments = validSegments))

        val t19Pt = dartboard.getNonEdgePointForSegment(19, SegmentType.TREBLE)
        dartboard.getColor(t19Pt) shouldBe GREY_COLOUR_WRAPPER.getColour(3, 19)

        val bullPt = dartboard.getNonEdgePointForSegment(25, SegmentType.DOUBLE)
        dartboard.getColor(bullPt) shouldBe GREY_COLOUR_WRAPPER.getBullColour(2)
    }

    @Test
    fun `Should colour segments that are invalid in black`()
    {
        val dartboard = DartzeeDartboard(150, 150)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)

        dartboard.refreshValidSegments(makeSegmentStatus(scoringSegments = emptyList(), validSegments = emptyList()))

        val t19Pt = dartboard.getNonEdgePointForSegment(19, SegmentType.TREBLE)
        dartboard.getColor(t19Pt) shouldBe Color.BLACK

        val bullPt = dartboard.getNonEdgePointForSegment(25, SegmentType.DOUBLE)
        dartboard.getColor(bullPt) shouldBe Color.BLACK
    }

    @Test
    fun `Should leave miss segments alone regardless of whether missing is allowed`()
    {
        val dartboard = DartzeeDartboard(100, 100)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)
        dartboard.refreshValidSegments(makeSegmentStatus(listOf(trebleNineteen, miss)))

        var missTwentyPts = dartboard.getPointsForSegment(20, SegmentType.MISS)
        missTwentyPts.forEach { dartboard.getColor(it) shouldBe Color.BLACK }

        dartboard.refreshValidSegments(makeSegmentStatus(emptyList()))
        missTwentyPts = dartboard.getPointsForSegment(20, SegmentType.MISS)
        missTwentyPts.forEach { dartboard.getColor(it) shouldBe Color.BLACK }

        dartboard.getColor(Point(0, 0)) shouldBe DartsColour.TRANSPARENT
    }

    @Test
    fun `Should not highlight invalid segments on hover`()
    {
        val dartboard = DartzeeDartboard(150, 150)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)
        dartboard.refreshValidSegments(makeSegmentStatus(listOf(trebleNineteen)))

        dartboard.ensureListening()

        val pt = dartboard.getNonEdgePointForSegment(20, SegmentType.DOUBLE)
        dartboard.highlightDartboard(pt)

        dartboard.getColor(pt) shouldBe Color.BLACK
    }

    @Test
    fun `Should put a border around scoring segments, but not valid or invalid segments`()
    {
        val dartboard = DartzeeDartboard(150, 150)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)
        dartboard.refreshValidSegments(makeSegmentStatus(scoringSegments = listOf(trebleNineteen), validSegments = listOf(trebleNineteen, doubleNineteen)))

        val scoringEdgePt = dartboard.getEdgePointForSegment(19, SegmentType.TREBLE)
        dartboard.getColor(scoringEdgePt) shouldBe Color.GRAY

        val validEdgePt = dartboard.getEdgePointForSegment(19, SegmentType.DOUBLE)
        dartboard.getColor(validEdgePt) shouldBe GREY_COLOUR_WRAPPER.getColour(2, 19)

        val invalidEdgePt = dartboard.getEdgePointForSegment(20, SegmentType.OUTER_SINGLE)
        dartboard.getColor(invalidEdgePt) shouldBe Color.BLACK
    }

    @Test
    fun `Should highlight scoring & valid segments on hover`()
    {
        val dartboard = DartzeeDartboard(150, 150)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)
        dartboard.refreshValidSegments(makeSegmentStatus(scoringSegments = listOf(doubleTwenty), validSegments = listOf(singleTwenty, doubleTwenty)))

        dartboard.ensureListening()

        val pt = dartboard.getNonEdgePointForSegment(20, SegmentType.DOUBLE)
        dartboard.highlightDartboard(pt)
        dartboard.getColor(pt) shouldBe DartsColour.getDarkenedColour(Color.RED)

        val validPt = dartboard.getNonEdgePointForSegment(20, SegmentType.INNER_SINGLE)
        dartboard.highlightDartboard(validPt)
        dartboard.getColor(validPt) shouldBe DartsColour.getDarkenedColour(GREY_COLOUR_WRAPPER.getColour(1, 20))
    }

    @Test
    fun `Snapshot test`()
    {
        val scoringSegments = getAllPossibleSegments().filter { it.score == 20 && !it.isMiss() }
        val validSegments = scoringSegments + getAllPossibleSegments().filter { it.score % 5 == 0 && !it.isMiss() }
        val segmentStatus = makeSegmentStatus(
            scoringSegments = scoringSegments,
            validSegments = validSegments
        )

        val dartboard = DartzeeDartboard(300, 300)
        dartboard.paintDartboard(DEFAULT_COLOUR_WRAPPER)
        dartboard.refreshValidSegments(segmentStatus)
        dartboard.shouldMatchImage("dartzee")
    }

    private fun Dartboard.getEdgePointForSegment(score: Int, segmentType: SegmentType): Point
    {
        val segment = getSegment(score, segmentType)!!
        segment.isEdgePoint(Point(0, 0))
        return segment.edgePoints.first()
    }
    private fun Dartboard.getNonEdgePointForSegment(score: Int, segmentType: SegmentType): Point
    {
        val segment = getSegment(score, segmentType)!!
        segment.isEdgePoint(Point(0, 0))
        val innerPoints = segment.points - segment.edgePoints
        return innerPoints.first()
    }
}