package dartzee.db

import dartzee.utils.Database
import dartzee.utils.InjectedThings.mainDatabase

class PendingLogsEntity(database: Database = mainDatabase): AbstractEntity<PendingLogsEntity>(database)
{
    //DB Fields
    var logJson = ""

    override fun getTableName() = "PendingLogs"

    override fun getCreateTableSqlSpecific() = "LogJson varchar(30000) NOT NULL"

    companion object
    {
        fun factory(logJson: String): PendingLogsEntity
        {
            val entity = PendingLogsEntity()
            entity.assignRowId()
            entity.logJson = logJson
            return entity
        }
    }
}