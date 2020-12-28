package dartzee.`object`

import dartzee.*
import dartzee.helper.AbstractTest
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class TestDartboardSegment: AbstractTest()
{
    @Test
    fun `Should correctly report whether a segment type represents a miss`()
    {
        doubleNineteen.isMiss() shouldBe false
        singleTwenty.isMiss() shouldBe false
        missTwenty.isMiss() shouldBe true
        missedBoard.isMiss() shouldBe true
    }

    @Test
    fun `Should correctly report whether a segment is a double excluding bull`()
    {
        doubleNineteen.isDoubleExcludingBull() shouldBe true
        doubleTwenty.isDoubleExcludingBull() shouldBe true
        bullseye.isDoubleExcludingBull() shouldBe false
        outerBull.isDoubleExcludingBull() shouldBe false
        singleTwenty.isDoubleExcludingBull() shouldBe false
        trebleNineteen.isDoubleExcludingBull() shouldBe false
    }

    @Test
    fun `Should report the correct multiplier`()
    {
        doubleNineteen.getMultiplier() shouldBe 2
        trebleNineteen.getMultiplier() shouldBe 3
        singleTwenty.getMultiplier() shouldBe 1
        missedBoard.getMultiplier() shouldBe 0
        missTwenty.getMultiplier() shouldBe 0
    }

    @Test
    fun `Should compute the segment score correctly`()
    {
        doubleNineteen.getTotal() shouldBe 38
        trebleTwenty.getTotal() shouldBe 60
        singleEighteen.getTotal() shouldBe 18
        missedBoard.getTotal() shouldBe 0
        missTwenty.getTotal() shouldBe 0
    }
}