package dartzee.screen

import dartzee.`object`.ColourWrapper
import dartzee.`object`.DEFAULT_COLOUR_WRAPPER
import dartzee.`object`.DartboardSegment
import dartzee.`object`.StatefulSegment
import dartzee.utils.DartsColour
import dartzee.utils.getColourForSegment
import java.awt.Color
import java.awt.Point
import java.awt.event.MouseEvent

class DartboardSegmentSelector(width: Int = 500, height: Int = 500): Dartboard(width, height)
{
    var selectedSegments = mutableSetOf<DartboardSegment>()

    private var lastDraggedSegment: StatefulSegment? = null

    init
    {
        scoreLabelColor = Color.BLACK
        renderScoreLabels = true
    }

    fun initState(initialSelection: Set<DartboardSegment>)
    {
        initialSelection.forEach { dataSegment ->
            val mySegment = getSegment(dataSegment.score, dataSegment.type) ?: return

            selectedSegments.add(dataSegment)

            val col = getColourForSegment(dataSegment, DEFAULT_COLOUR_WRAPPER)
            colourSegment(mySegment, col)
        }
    }


    fun paintDartboard()
    {
        val wireframe = ColourWrapper(DartsColour.TRANSPARENT)
        wireframe.edgeColour = Color.BLACK
        super.paintDartboard(wireframe, true, cached=false)
    }

    override fun dartThrown(pt: Point)
    {
        val segment = getSegmentForPoint(pt)
        toggleSegment(segment)
    }

    private fun toggleSegment(segment: StatefulSegment?)
    {
        lastDraggedSegment = segment
        if (segment == null || segment.isMiss())
        {
            return
        }

        val dataSegment = segment.toDataSegment()
        if (selectedSegments.contains(dataSegment))
        {
            selectedSegments.remove(dataSegment)

            colourSegment(segment, DartsColour.TRANSPARENT)
        }
        else
        {
            selectedSegments.add(dataSegment)

            val col = getColourForSegment(dataSegment, DEFAULT_COLOUR_WRAPPER)
            colourSegment(segment, col)
        }
    }

    override fun mouseMoved(arg0: MouseEvent)
    {
        //Do nothing
    }
    override fun mouseDragged(arg0: MouseEvent)
    {
        val segment = getSegmentForPoint(arg0.point)
        if (segment == lastDraggedSegment)
        {
            return
        }

        toggleSegment(segment)
    }

}