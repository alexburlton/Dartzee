package dartzee.utils

import dartzee.core.screen.ProgressDialog
import dartzee.db.DartzeeRuleEntity

object DartzeeRuleConversion
{
    fun convertDartzeeRules()
    {
        val allRules = DartzeeRuleEntity().retrieveEntities()

        val r = Runnable {
            val dlg = ProgressDialog.factory("Converting Dartzee Rules",
                "rules", allRules.size)

            dlg.setVisibleLater()

            allRules.forEach {
                val dto = it.toDto(false)
                val newResult = dto.runStrengthCalculation()
                it.calculationResult = newResult.toDbString()
                it.saveToDatabase()

                dlg.incrementProgressLater()
            }

            dlg.disposeLater()
        }

        val t = Thread(r)
        t.start()
        t.join()
    }
}