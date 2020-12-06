package dartzee.dartzee.dart

import dartzee.*
import dartzee.dartzee.AbstractDartzeeRuleTest
import io.kotlintest.shouldBe
import org.junit.Test

class TestDartzeeDartRuleOdd: AbstractDartzeeRuleTest<DartzeeDartRuleOdd>()
{
    override fun factory() = DartzeeDartRuleOdd()

    @Test
    fun `segment validation`()
    {
        val rule = DartzeeDartRuleOdd()

        rule.isValidSegment(doubleNineteen) shouldBe true
        rule.isValidSegment(trebleTwenty) shouldBe false
        rule.isValidSegment(singleNineteen) shouldBe true
        rule.isValidSegment(miss) shouldBe false
        rule.isValidSegment(outerBull) shouldBe true
    }
}