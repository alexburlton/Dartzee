package dartzee.screen.stats.player

import dartzee.core.bean.ScrollTable
import dartzee.core.util.DateStatics
import dartzee.core.util.containsComponent
import dartzee.core.util.getSqlDateNow
import dartzee.helper.AbstractTest
import dartzee.stats.GameWrapper
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrowAny
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Component
import java.sql.Timestamp

abstract class AbstractPlayerStatisticsTest<E: AbstractStatisticsTab>: AbstractTest()
{
    abstract fun factoryTab(): E
    abstract fun getComponentsForComparison(tab: E): List<Component>

    @Test
    fun `Components for comparison should have red foregrounds`()
    {
        val components = getComponentsForComparison(factoryTab())
        components.forEach{
            when(it)
            {
                is ScrollTable -> it.tableForeground shouldBe Color.RED
            }
        }
    }

    @Test
    fun `Should show or hide comparison components`()
    {
        val tab = factoryTab()
        val components = getComponentsForComparison(tab)

        tab.setFilteredGames(listOf(constructGameWrapper()), listOf())
        tab.populateStats()
        components.forEach{
            tab.containsComponent(it) shouldBe false
        }

        tab.setFilteredGames(listOf(constructGameWrapper()), listOf(constructGameWrapper()))
        tab.populateStats()
        components.forEach{
            tab.containsComponent(it) shouldBe true
        }

        tab.setFilteredGames(listOf(constructGameWrapper()), listOf())
        tab.populateStats()
        components.forEach{
            tab.containsComponent(it) shouldBe false
        }
    }

    @Test
    fun `It should handle displaying no games`()
    {
        shouldNotThrowAny{
            val tab = factoryTab()
            tab.setFilteredGames(listOf(), listOf())

            tab.populateStats()
        }
    }

    fun constructGameWrapper(localId: Long = 1,
                             gameParams: String = "",
                             dtStart: Timestamp = getSqlDateNow(),
                             dtFinish: Timestamp = DateStatics.END_OF_TIME,
                             finalScore: Int = -1): GameWrapper
    {
        return GameWrapper(localId, gameParams, dtStart, dtFinish, finalScore)
    }
}