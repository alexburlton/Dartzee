package dartzee.db

import dartzee.core.util.DialogUtil
import dartzee.logging.CODE_MERGE_ERROR
import dartzee.utils.DartsDatabaseUtil
import dartzee.utils.Database
import dartzee.utils.InjectedThings

class ForeignDatabaseValidator(private val migrator: DatabaseMigrator)
{
    fun validateAndMigrateForeignDatabase(database: Database, desc: String): Boolean
    {
        if (!database.testConnection())
        {
            DialogUtil.showError("An error occurred connecting to the $desc database.")
            return false
        }

        val remoteVersion = database.getDatabaseVersion()
        if (remoteVersion == null)
        {
            InjectedThings.logger.error(CODE_MERGE_ERROR, "Unable to ascertain $desc database version (but could connect) - this is unexpected.")
            DialogUtil.showError("An error occurred connecting to the $desc database.")
            return false
        }

        if (remoteVersion > DartsDatabaseUtil.DATABASE_VERSION)
        {
            val error = "The $desc database contains data written by a higher Dartzee version. \n\nYou will need to update to the latest version of Dartzee before continuing."
            DialogUtil.showError(error)
            return false
        }

        val result = migrator.migrateToLatest(database, desc.capitalize())
        if (result != MigrationResult.SUCCESS)
        {
            return false
        }

        return true
    }
}