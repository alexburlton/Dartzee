package dartzee.achievements

import dartzee.`object`.*
import dartzee.db.AchievementEntity
import dartzee.db.BulkInserter
import dartzee.game.GameType
import dartzee.utils.Database
import java.sql.ResultSet

const val X01_ROUNDS_TABLE = "X01Rounds"
const val LAST_ROUND_FROM_PARTICIPANT = "CEIL(CAST(pt.FinalScore AS DECIMAL)/3)"

fun getGolfSegmentCases(): String
{
    val sb = StringBuilder()
    sb.append(" WHEN drt.SegmentType = '${SegmentType.DOUBLE}' THEN 1")
    sb.append(" WHEN drt.SegmentType = '${SegmentType.TREBLE}' THEN 2")
    sb.append(" WHEN drt.SegmentType = '${SegmentType.INNER_SINGLE}' THEN 3")
    sb.append(" WHEN drt.SegmentType = '${SegmentType.OUTER_SINGLE}' THEN 4")
    sb.append(" ELSE 5")

    return sb.toString()
}

fun appendPlayerSql(sb: StringBuilder, playerIds: List<String>, alias: String? = "pt", whereOrAnd: String = "AND")
{
    if (playerIds.isEmpty())
    {
        return
    }

    val keys = playerIds.joinToString { "'$it'" }
    val column = if (alias != null) "$alias.PlayerId" else "PlayerId"
    sb.append(" $whereOrAnd $column IN ($keys)")
}

fun ensureX01RoundsTableExists(playerIds: List<String>, database: Database)
{
    val created = database.createTableIfNotExists(
        X01_ROUNDS_TABLE,
        "PlayerId VARCHAR(36), GameId VARCHAR(36), ParticipantId VARCHAR(36), StartingScore INT, RoundNumber INT, " +
            "TotalDartsThrown INT, RemainingScore INT, LastDartScore INT, LastDartMultiplier INT, DtRoundFinished TIMESTAMP")

    if (!created)
    {
        return
    }

    val tmp1 = database.createTempTable("X01RoundsPt1",
        "PlayerId VARCHAR(36), GameId VARCHAR(36), ParticipantId VARCHAR(36), StartingScore INT, RoundNumber INT, LastDartOrdinal INT")

    var sb = StringBuilder()
    sb.append(" INSERT INTO $tmp1")
    sb.append(" SELECT pt.PlayerId, pt.GameId, pt.RowId, drtFirst.StartingScore, drtFirst.RoundNumber, MAX(drt.Ordinal)")
    sb.append(" FROM Dart drtFirst, Participant pt, Game g, Dart drt")
    sb.append(" WHERE drtFirst.ParticipantId = pt.RowId")
    sb.append(" AND drtFirst.PlayerId = pt.PlayerId")
    sb.append(" AND pt.GameId = g.RowId")
    sb.append(" AND g.GameType = '${GameType.X01}'")
    sb.append(" AND drtFirst.Ordinal = 1")
    sb.append(" AND drtFirst.PlayerId = drt.PlayerId")
    sb.append(" AND drtFirst.ParticipantId = drt.ParticipantId")
    sb.append(" AND drtFirst.RoundNumber = drt.RoundNumber")
    appendPlayerSql(sb, playerIds)
    sb.append(" GROUP BY pt.PlayerId, pt.GameId, pt.RowId, drtFirst.StartingScore, drtFirst.RoundNumber")
    database.executeUpdate(sb.toString())

    sb = StringBuilder()
    sb.append(" INSERT INTO $X01_ROUNDS_TABLE")
    sb.append(" SELECT zz.PlayerId, zz.GameId, zz.ParticipantId, zz.StartingScore, zz.RoundNumber, zz.LastDartOrdinal,")
    sb.append(" drt.StartingScore - (drt.Score * drt.Multiplier), drt.Score, drt.Multiplier, drt.DtCreation")
    sb.append(" FROM $tmp1 zz, Dart drt")
    sb.append(" WHERE zz.PlayerId = drt.PlayerId")
    sb.append(" AND zz.ParticipantId = drt.ParticipantId")
    sb.append(" AND zz.RoundNumber = drt.RoundNumber")
    sb.append(" AND zz.LastDartOrdinal = drt.Ordinal")
    database.executeUpdate(sb.toString())
}

fun bulkInsertFromResultSet(rs: ResultSet,
                            database: Database,
                            achievementType: AchievementType,
                            achievementDetailFn: (() -> String)? = null,
                            achievementCounterFn: (() -> Int)? = null,
                            oneRowPerPlayer: Boolean = false)
{
    val playerIdsSeen = mutableSetOf<String>()

    val entities = mutableListOf<AchievementEntity>()
    while (rs.next())
    {
        val playerId = rs.getString("PlayerId")
        val gameId = rs.getString("GameId")
        val dtAchieved = rs.getTimestamp("DtAchieved")
        val detail = achievementDetailFn?.invoke() ?: ""
        val counter = achievementCounterFn?.invoke() ?: -1

        if (!oneRowPerPlayer || playerIdsSeen.add(playerId))
        {
            entities.add(AchievementEntity.factory(achievementType, playerId, gameId, counter, detail, dtAchieved, database))
        }
    }

    BulkInserter.insert(entities, database = database)
}