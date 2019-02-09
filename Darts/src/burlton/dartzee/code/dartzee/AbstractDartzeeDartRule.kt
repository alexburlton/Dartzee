package burlton.dartzee.code.dartzee

import burlton.core.code.util.XmlUtil
import burlton.dartzee.code.`object`.DartboardSegmentKt
import org.w3c.dom.Element

abstract class AbstractDartzeeDartRule
{
    abstract fun isValidSegment(segment: DartboardSegmentKt) : Boolean

    abstract fun getRuleIdentifier(): String

    abstract fun writeXmlAttributes(rootElement: Element)
    abstract fun populate(rootElement: Element)

    open fun getDisplayName(): String = getRuleIdentifier()
    open fun isValid(): Boolean = true

    override fun toString() = getDisplayName()

    fun toDbString(): String
    {
        val xmlDoc = XmlUtil.factoryNewDocument()
        xmlDoc ?: return "<NULL>"

        val rootElement = xmlDoc.createElement(getRuleIdentifier())
        writeXmlAttributes(rootElement)

        xmlDoc.appendChild(rootElement)
        return XmlUtil.getStringFromDocument(xmlDoc)
    }
}

fun getAllDartRules(): MutableList<AbstractDartzeeDartRule>
{
    return mutableListOf(DartzeeDartRuleEven(),
                         DartzeeDartRuleOdd(),
                         DartzeeRuleInner(),
                         DartzeeRuleOuter(),
                         DartzeeDartRuleColour())
}

fun parseDartzeeRule(xmlStr: String): AbstractDartzeeDartRule?
{
    val xmlDoc = XmlUtil.getDocumentFromXmlString(xmlStr)
    xmlDoc ?: return null

    val rootElement = xmlDoc.documentElement

    val rule = getAllDartRules().find{it.getRuleIdentifier() == rootElement.tagName}
    rule ?: return null

    rule.populate(rootElement)

    return rule
}