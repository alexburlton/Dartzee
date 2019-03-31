package burlton.dartzee.code.db

import burlton.dartzee.code.utils.DartsDatabaseUtil

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class VersionEntity : AbstractEntity<VersionEntity>()
{
    var version = DartsDatabaseUtil.DATABASE_VERSION

    override fun getTableName() = "Version"

    override fun getCreateTableSqlSpecific() = "Version INT NOT NULL"

    @Throws(SQLException::class)
    override fun populateFromResultSet(entity: VersionEntity, rs: ResultSet)
    {
        entity.version = rs.getInt("Version")
    }

    @Throws(SQLException::class)
    override fun writeValuesToStatement(statement: PreparedStatement, startIndex: Int, emptyStatement: String): String
    {
        var statementStr = emptyStatement
        statementStr = writeInt(statement, startIndex, version, statementStr)
        return statementStr
    }

    companion object
    {
        fun retrieveCurrentDatabaseVersion(): VersionEntity?
        {
            val entities = VersionEntity().retrieveEntities("1 = 1")
            return if (entities.isEmpty()) null else entities[0]
        }
    }
}