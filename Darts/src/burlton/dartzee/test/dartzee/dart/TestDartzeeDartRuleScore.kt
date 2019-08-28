package burlton.dartzee.test.dartzee.dart

import burlton.dartzee.code.dartzee.dart.DartzeeDartRuleScore
import burlton.dartzee.code.dartzee.parseDartRule
import burlton.dartzee.test.*
import burlton.dartzee.test.dartzee.AbstractDartzeeRuleTest
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.numerics.shouldNotBeInRange
import io.kotlintest.shouldBe
import org.junit.Test

class TestDartzeeDartRuleScore: AbstractDartzeeRuleTest<DartzeeDartRuleScore>()
{
    override fun factory() = DartzeeDartRuleScore()

    @Test
    fun `segment validation`()
    {
        val rule = DartzeeDartRuleScore()
        rule.score = 20

        rule.isValidSegment(singleTwenty) shouldBe true
        rule.isValidSegment(doubleTwenty) shouldBe true
        rule.isValidSegment(trebleTwenty) shouldBe true
        rule.isValidSegment(trebleNineteen) shouldBe false
        rule.isValidSegment(missTwenty) shouldBe false
    }

    @Test
    fun `Score config panel updates rule correctly`()
    {
        val rule = DartzeeDartRuleScore()
        rule.spinner.value shouldBe rule.score
        rule.score shouldBeGreaterThan -1

        for (i in 1..25)
        {
            rule.spinner.value = i
            rule.stateChanged(null)

            rule.spinner.value shouldBe rule.score
            rule.score shouldNotBeInRange(21..24)
        }
    }

    @Test
    fun `Read and write XML`()
    {
        val rule = DartzeeDartRuleScore()
        rule.score = 18

        val xml = rule.toDbString()
        val parsedRule = parseDartRule(xml) as DartzeeDartRuleScore

        parsedRule.score shouldBe 18
        parsedRule.spinner.value shouldBe 18
    }
}