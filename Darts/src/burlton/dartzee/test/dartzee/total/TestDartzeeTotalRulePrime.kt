package burlton.dartzee.test.dartzee.total

import burlton.dartzee.code.dartzee.total.DartzeeTotalRulePrime
import burlton.dartzee.test.dartzee.AbstractDartzeeRuleTest
import io.kotlintest.shouldBe
import org.junit.Test

class TestDartzeeTotalRulePrime: AbstractDartzeeRuleTest<DartzeeTotalRulePrime>()
{
    override fun factory() = DartzeeTotalRulePrime()

    @Test
    fun `Should always return true for potentially valid`()
    {
        DartzeeTotalRulePrime().isPotentiallyValidTotal(100, 0) shouldBe true
    }

    @Test
    fun `Total validation`()
    {
        val rule = DartzeeTotalRulePrime()

        rule.isValidTotal(2) shouldBe true
        rule.isValidTotal(3) shouldBe true
        rule.isValidTotal(7) shouldBe true
        rule.isValidTotal(23) shouldBe true

        rule.isValidTotal(6) shouldBe false
        rule.isValidTotal(21) shouldBe false
    }

}