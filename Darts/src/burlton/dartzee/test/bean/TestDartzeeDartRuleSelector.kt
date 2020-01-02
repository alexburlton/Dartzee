package burlton.dartzee.test.bean

import burlton.dartzee.code.bean.DartzeeDartRuleSelector
import burlton.dartzee.code.dartzee.getAllDartRules
import burlton.dartzee.test.helper.AbstractDartsTest
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldNotContain
import io.kotlintest.shouldBe
import org.junit.Test

class TestDartzeeDartRuleSelector : AbstractDartsTest()
{
    @Test
    fun `Should initialise with all the dart rules`()
    {
        val selector = DartzeeDartRuleSelector("")
        selector.getRules().size shouldBe getAllDartRules().size
    }

    @Test
    fun `Should not be optional`()
    {
        val selector = DartzeeDartRuleSelector("")
        selector.isOptional() shouldBe false
        selector.shouldBeEnabled() shouldBe true

        val children = selector.components.toList()
        children.shouldContain(selector.lblDesc)
        children.shouldNotContain(selector.cbDesc)
    }
}