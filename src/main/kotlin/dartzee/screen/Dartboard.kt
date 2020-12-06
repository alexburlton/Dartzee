package dartzee.screen

import dartzee.`object`.*
import dartzee.core.bean.getPointList
import dartzee.core.bean.paintOffset
import dartzee.core.util.getParentWindow
import dartzee.core.util.runOnEventThreadBlocking
import dartzee.listener.DartboardListener
import dartzee.logging.CODE_RENDERED_DARTBOARD
import dartzee.logging.KEY_CACHED
import dartzee.logging.KEY_DURATION
import dartzee.screen.game.DartsGameScreen
import dartzee.utils.*
import dartzee.utils.DartsColour.DARTBOARD_BLACK
import dartzee.utils.InjectedThings.logger
import dartzee.utils.ResourceCache.BASE_FONT
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.sound.sampled.Clip
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JLayeredPane
import javax.swing.SwingConstants
import kotlin.math.round

const val LAYER_DARTS = 2
const val LAYER_DODGY = 3
const val LAYER_SLIDER = 4

open class Dartboard(width: Int = 400, height: Int = 400): JLayeredPane(), MouseListener, MouseMotionListener
{
    protected val hmSegmentKeyToSegment = mutableMapOf<String, StatefulSegment>()

    private val dartLabels = mutableListOf<JLabel>()

    private val listeners: MutableList<DartboardListener> = mutableListOf()
    var renderDarts = false
    var centerPoint = Point(200, 200)
        private set

    private var diameter = 360.0

    var scoreLabelColor: Color = Color.WHITE
    var renderScoreLabels = false

    private var dartCount = 0
    var simulation = false

    //Cached things
    private var lastHoveredSegment: StatefulSegment? = null
    private var colourWrapper: ColourWrapper? = null

    //For dodgy sounds/animations
    var latestClip: Clip? = null
    val dodgyLabel = JLabel()

    var dartboardImage: BufferedImage? = null
    val dartboardLabel = JLabel()

    init
    {
        setSize(width, height)
        preferredSize = Dimension(width, height)
        dartboardLabel.setSize(width, height)
        layout = null
        add(dartboardLabel, Integer.valueOf(-1))
    }

    fun addDartboardListener(listener: DartboardListener)
    {
        listeners.add(listener)
    }

    fun paintDartboardCached()
    {
        paintDartboard(cached = true)
    }

    open fun paintDartboard(colourWrapper: ColourWrapper? = null, listen: Boolean = true, cached: Boolean = false)
    {
        val width = width
        val height = height

        val timer = DurationTimer()
        val usingCache = cached && dartboardTemplate != null

        dartboardLabel.setSize(width, height)

        //Initialise/clear down variables
        this.colourWrapper = colourWrapper
        centerPoint = Point(width / 2, height / 2)
        diameter = 0.7 * width

        //Construct the segments, populated with their points. Cache pt -> segment.
        getPointList(width, height, ((width - diameter)/2).toInt()).forEach { factoryAndCacheSegmentForPoint(it) }
        getAllSegments().forEach { it.computeEdgePoints() }

        if (usingCache)
        {
            initialiseFromTemplate()
        }
        else
        {
            paintDartboardImage()
        }

        addScoreLabels()

        dartboardLabel.icon = ImageIcon(dartboardImage!!)
        dartboardLabel.repaint()

        if (cached
          && dartboardTemplate == null)
        {
            dartboardTemplate = DartboardTemplate(this)
        }

        val duration = timer.getDuration()
        logger.info(CODE_RENDERED_DARTBOARD, "Rendered dartboard[$width, $height] in ${duration}ms (cached: $usingCache)",
            KEY_DURATION to duration, KEY_CACHED to usingCache)

        //Now the dartboard is painted, add the mouse listeners
        if (listen)
        {
            ensureListening()
        }
    }

    private fun paintDartboardImage()
    {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // 1) Paint the whole thing the background colour (usually transparent)
        val backgroundColour = colourWrapper?.missedBoardColour ?: DartsColour.TRANSPARENT
        val g = img.graphics.create() as Graphics2D
        g.color = backgroundColour
        g.fillRect(0, 0, width, height)
        g.dispose()

        // 2) Paint the outer dartboard piece if appropriate
        val outerDartboardColour = colourWrapper?.outerDartboardColour ?: Color.BLACK
        if (backgroundColour != outerDartboardColour)
        {
            val thickness = (diameter * 0.16).toFloat()
            val outerG = img.graphics.create() as Graphics2D
            outerG.color = colourWrapper?.outerDartboardColour ?: Color.BLACK
            outerG.stroke = BasicStroke(thickness)

            val startX = centerPoint.x - (diameter * 1.15)/2
            val startY = centerPoint.y - (diameter * 1.15)/2
            outerG.drawOval(startX.toInt(), startY.toInt(), (diameter * 1.15).toInt(), (diameter * 1.15).toInt())
            outerG.dispose()
        }

        // 3) Paint the rest according to our StatefulSegments
        val offset = ((width - diameter)/2).toInt()
        val hmPointToColor = getAllSegments().flatMap { it.getColorMap(colourWrapper) }.toMap()
        img.paintOffset(offset) { hmPointToColor[it] ?: colourWrapper?.missedBoardColour }

        dartboardImage = img
    }

    fun initialiseFromTemplate()
    {
        dartboardImage = dartboardTemplate!!.getDartboardImg()
    }

    private fun addScoreLabels()
    {
        if (!renderScoreLabels)
        {
            return
        }

        //Get the height we want for our labels, which is half the thickness of the outer band
        val radius = diameter / 2
        val outerRadius = UPPER_BOUND_OUTSIDE_BOARD_RATIO * radius
        val lblHeight = round((outerRadius - radius) / 2).toInt()

        val fontToUse = getFontForDartboardLabels(lblHeight)

        for (i in 1..20)
        {
            //Create a label with standard properties
            val lbl = JLabel("" + i)
            lbl.foreground = scoreLabelColor
            lbl.horizontalAlignment = SwingConstants.CENTER
            lbl.font = fontToUse

            //Work out the width for this label, based on the text
            val metrics = factoryFontMetrics(fontToUse)
            val lblWidth = metrics.stringWidth("" + i) + 2
            lbl.setSize(lblWidth, lblHeight)

            //Work out where to place the label
            val points = getPointsForSegment(i, SegmentType.MISS)
            val avgPoint = getAverage(points)
            val lblX = avgPoint.getX().toInt() - lblWidth / 2
            val lblY = avgPoint.getY().toInt() - lblHeight / 2

            val g = dartboardImage!!.graphics.create() as Graphics2D
            g.color = Color.WHITE
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.translate(lblX, lblY)
            lbl.paint(g)
        }
    }

    private fun getFontForDartboardLabels(lblHeight: Int): Font
    {
        //Start with a fontSize of 1
        var fontSize = 1f
        var font = BASE_FONT.deriveFont(Font.PLAIN, fontSize)

        //We're going to increment our test font 1 at a time, and keep checking its height
        var testFont = font
        var metrics = factoryFontMetrics(testFont)
        var fontHeight = metrics.height

        while (fontHeight < lblHeight - 2)
        {
            //The last iteration succeeded, so set our return value to be the font we tested.
            font = testFont

            //Create a new testFont, with incremented font size
            fontSize++
            testFont = BASE_FONT.deriveFont(Font.PLAIN, fontSize)

            //Get the updated font height
            metrics = factoryFontMetrics(testFont)
            fontHeight = metrics.height
        }

        return font
    }

    private fun factoryFontMetrics(font: Font): FontMetrics
    {
        //Use a new Canvas rather than going via graphics, as then this will work headless (e.g. from tests)
        return Canvas().getFontMetrics(font)
    }

    fun highlightDartboard(hoveredPoint: Point)
    {
        val hoveredSegment = getSegmentForPoint(hoveredPoint)
        if (hoveredSegment == lastHoveredSegment)
        {
            //Nothing to do
            return
        }

        lastHoveredSegment?.let { colourSegment(it, false) }
        lastHoveredSegment = hoveredSegment
        hoveredSegment?.let { colourSegment(it, true) }
    }

    open fun getInitialColourForSegment(segment: DartboardSegment) = getColourForSegment(segment, colourWrapper)

    fun colourSegment(segment: StatefulSegment, highlight: Boolean)
    {
        val dataSegment = segment.toDataSegment()
        if (segment.isMiss())
        {
            return
        }

        val colour = getInitialColourForSegment(dataSegment)
        val hoveredColour = if (highlight && shouldActuallyHighlight(dataSegment)) getHighlightedColour(colour) else colour

        colourSegment(segment, hoveredColour)
    }
    private fun getHighlightedColour(colour: Color): Color =
        if (colour == DARTBOARD_BLACK)
        {
            Color.DARK_GRAY
        }
        else
        {
            DartsColour.getDarkenedColour(colour)
        }

    open fun shouldActuallyHighlight(segment: DartboardSegment) = true

    fun colourSegment(segment: StatefulSegment, col: Color)
    {
        val pointsForCurrentSegment = segment.points
        val edgeColour = getEdgeColourForSegment(segment.toDataSegment())
        for (pt in pointsForCurrentSegment)
        {
            if (edgeColour != null && segment.isEdgePoint(pt))
            {
                colourPoint(pt, edgeColour)
            }
            else
            {
                colourPoint(pt, col)
            }
        }

        dartboardLabel.repaint()
    }

    open fun getEdgeColourForSegment(segment: DartboardSegment) = colourWrapper?.edgeColour

    private fun colourPoint(pt: Point, colour: Color)
    {
        val x = pt.getX().toInt()
        val y = pt.getY().toInt()

        val rgb = colour.rgb
        val currentRgb = dartboardImage!!.getRGB(x, y)

        if (rgb != currentRgb)
        {
            dartboardImage!!.setRGB(x, y, rgb)
        }
    }

    fun getAllSegments() = hmSegmentKeyToSegment.values.toList()

    fun getDataSegmentForPoint(pt: Point): DartboardSegment
    {
        val segment = getSegmentForPoint(pt)
        return segment?.toDataSegment() ?: DartboardSegment(SegmentType.MISS, 0)
    }

    fun getSegmentForPoint(pt: Point): StatefulSegment?
    {
        val segment = getAllSegments().firstOrNull { it.containsPoint(pt) }
        if (segment != null)
        {
            return segment
        }

        return null
    }

    private fun factoryAndCacheSegmentForPoint(pt: Point): StatefulSegment?
    {
        val newSegment = factorySegmentForPoint(pt, centerPoint, diameter) ?: return null

        val segmentKey = "${newSegment.score}_${newSegment.type}"
        val segment = hmSegmentKeyToSegment.getOrPut(segmentKey) { newSegment }
        segment.addPoint(pt)
        return segment
    }

    /**
     * Public methods
     */
    fun getPointsForSegment(score: Int, type: SegmentType): Set<Point>
    {
        val segmentKey = score.toString() + "_" + type
        val segment = hmSegmentKeyToSegment[segmentKey]
        return segment?.points ?: setOf()
    }
    fun getSegment(score: Int, type: SegmentType): StatefulSegment? = hmSegmentKeyToSegment["${score}_$type"]

    fun isDouble(pt: Point): Boolean
    {
        val seg = getSegmentForPoint(pt)
        return seg?.isDoubleExcludingBull() ?: false
    }

    fun getPotentialAimPoints() = getPotentialAimPoints(centerPoint, diameter)
    fun translateAimPoint(aimPoint: AimPoint) = AimPoint(centerPoint, diameter / 2, aimPoint.angle, aimPoint.ratio).point

    open fun dartThrown(pt: Point)
    {
        val dart = convertPointToDart(pt, true)

        if (renderDarts)
        {
            runOnEventThreadBlocking { addDart(pt) }
        }

        listeners.forEach {
            it.dartThrown(dart)
        }
    }

    fun addOverlay(pt: Point, overlay: Component)
    {
        add(overlay)
        setLayer(overlay, LAYER_SLIDER)
        overlay.location = pt
    }

    fun convertPointToDart(pt: Point, rationalise: Boolean): Dart
    {
        if (rationalise)
        {
            rationalisePoint(pt)
        }

        val segment = getDataSegmentForPoint(pt)
        return getDartForSegment(pt, segment)
    }

    fun rationalisePoint(pt: Point)
    {
        val x = pt.x.coerceIn(0, width - 1)
        val y = pt.y.coerceIn(0, height - 1)

        pt.setLocation(x, y)
    }

    fun listen(listen: Boolean)
    {
        if (listen)
        {
            ensureListening()
        }
        else
        {
            stopListening()
        }
    }

    fun ensureListening()
    {
        if (dartboardLabel.mouseListeners.isEmpty())
        {
            dartboardLabel.addMouseListener(this)
            dartboardLabel.addMouseMotionListener(this)
        }
    }

    fun stopListening()
    {
        if (dartboardLabel.mouseListeners.isNotEmpty())
        {
            dartboardLabel.removeMouseListener(this)
            dartboardLabel.removeMouseMotionListener(this)
        }

        //Undo any colouring that there might have been
        lastHoveredSegment?.let { colourSegment(it, false) }
    }

    private fun addDart(pt: Point)
    {
        if (dartLabels.isEmpty())
        {
            for (i in 0..4)
            {
                val lbl = JLabel(DARTIMG)
                lbl.setSize(76, 80)
                lbl.isVisible = false
                add(lbl)

                dartLabels.add(lbl)
            }
        }

        val lbl = dartLabels[dartCount]
        lbl.location = pt
        lbl.isVisible = true
        dartCount++

        setLayer(lbl, LAYER_DARTS, 5-dartCount)

        revalidate()
        repaint()
    }

    fun clearDarts()
    {
        //Always want to stop this at the same time
        //stopDodgy();

        for (i in dartLabels.indices)
        {
            val dartLabel = dartLabels[i]
            dartLabel.isVisible = false
        }

        dartCount = 0
        revalidate()
        repaint()
    }

    override fun mouseMoved(arg0: MouseEvent)
    {
        if (getParentWindow()?.isFocused == true)
        {
            highlightDartboard(arg0.point)
        }
    }

    override fun mouseReleased(arg0: MouseEvent)
    {
        if (!suppressClickForGameWindow())
        {
            val pt = arg0.point
            dartThrown(pt)
        }
    }
    private fun suppressClickForGameWindow(): Boolean
    {
        val scrn = getParentWindow() as? DartsGameScreen ?: return false
        if (scrn.haveLostFocus)
        {
            scrn.haveLostFocus = false
            return true
        }

        return false
    }

    override fun mouseClicked(arg0: MouseEvent) {}
    override fun mousePressed(arg0: MouseEvent) {}
    override fun mouseDragged(arg0: MouseEvent) {}
    override fun mouseEntered(arg0: MouseEvent) {}
    override fun mouseExited(arg0: MouseEvent) {}

    fun factoryOverlay() = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    companion object
    {
        private val DARTIMG = ImageIcon(Dartboard::class.java.getResource("/dartImage.png"))
        var dartboardTemplate: DartboardTemplate? = null

        fun appearancePreferenceChanged()
        {
            dartboardTemplate = null
        }
    }

    inner class DartboardTemplate(dartboard: Dartboard)
    {
        private val dartboardImg = dartboard.dartboardImage!!

        fun getDartboardImg(): BufferedImage
        {
            val cm = dartboardImg.colorModel
            val isAlphaPremultiplied = cm.isAlphaPremultiplied
            val writableRaster = dartboardImg.raster.createCompatibleWritableRaster()
            val raster = dartboardImg.copyData(writableRaster)
            return BufferedImage(cm, raster, isAlphaPremultiplied, null)
        }

    }

}
