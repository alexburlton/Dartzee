package dartzee.`object`

import dartzee.core.util.getAttributeInt
import dartzee.core.util.setAttributeAny
import org.w3c.dom.Element

const val MISS_FUDGE_FACTOR = 5126

/**
 * Data class so that equivalent segments are treated as equal (e.g. DartzeeRuleCalculationResult externalisation)
 */
data class DartboardSegment(val type: SegmentType, val score: Int)
{
    /**
     * Helpers
     */
    fun isMiss() = type == SegmentType.MISS
    fun isDoubleExcludingBull() = type == SegmentType.DOUBLE && score != 25
    fun getMultiplier() = type.getMultiplier()
    fun getTotal(): Int = score * getMultiplier()

    override fun toString() = "$score ($type)"

    fun getRoughProbability(): Double {
        return type.getRoughSize(score).toDouble() / getRoughScoringArea()
    }

    fun writeXml(root: Element, name: String)
    {
        val element = root.ownerDocument.createElement(name)
        element.setAttributeAny("Score", score)
        element.setAttributeAny("Type", type)

        root.appendChild(element)
    }

    companion object
    {
        fun readList(root: Element, itemName: String): List<DartboardSegment>
        {
            val list = root.getElementsByTagName(itemName)

            return (0 until list.length).map {
                val node = list.item(it) as Element
                val score = node.getAttributeInt("Score")
                val type = SegmentType.valueOf(node.getAttribute("Type"))
                DartboardSegment(type, score)
            }
        }
    }
}

