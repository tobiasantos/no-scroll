package br.ufrn.noscroll.adapters.persistence

import br.ufrn.noscroll.domain.AlertMode
import br.ufrn.noscroll.domain.NewSession
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.SessionRepository
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.SetupRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.sql.DataSource
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

data class DbConfig(val url: String, val user: String, val password: String) {
    companion object {
        fun fromEnv() = DbConfig(
            url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/noscroll",
            user = System.getenv("DB_USER") ?: "noscroll",
            password = System.getenv("DB_PASSWORD") ?: "noscroll",
        )
    }
}

fun createDataSource(config: DbConfig): HikariDataSource = HikariDataSource(
    HikariConfig().apply {
        jdbcUrl = config.url
        username = config.user
        password = config.password
        maximumPoolSize = 5
    },
)

fun migrate(dataSource: DataSource) {
    Flyway.configure().dataSource(dataSource).load().migrate()
}

object Setups : Table("setups") {
    val id = integer("id").autoIncrement()
    val appPackage = varchar("app_package", 200)
    val appName = varchar("app_name", 100)
    val dailyLimitMinutes = integer("daily_limit_minutes")
    val alertMode = enumerationByName("alert_mode", 10, AlertMode::class)
    val alertIntervalMinutes = integer("alert_interval_minutes").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Sessions : Table("sessions") {
    val id = integer("id").autoIncrement()
    val setupId = integer("setup_id").references(Setups.id, onDelete = ReferenceOption.CASCADE)
    val startedAt = timestampWithTimeZone("started_at")
    val durationMinutes = integer("duration_minutes")
    override val primaryKey = PrimaryKey(id)
}

class PostgresSetupRepository(private val db: Database) : SetupRepository {

    override suspend fun list(): List<Setup> = suspendTransaction(db) {
        Setups.selectAll().orderBy(Setups.id to SortOrder.ASC).map { it.toSetup() }
    }

    override suspend fun find(id: Int): Setup? = suspendTransaction(db) {
        Setups.selectAll().where { Setups.id eq id }.singleOrNull()?.toSetup()
    }

    override suspend fun add(new: NewSetup): Setup = suspendTransaction(db) {
        val id = Setups.insert {
            it[appPackage] = new.appPackage
            it[appName] = new.appName
            it[dailyLimitMinutes] = new.dailyLimitMinutes
            it[alertMode] = new.alertMode
            it[alertIntervalMinutes] = new.alertIntervalMinutes
        } get Setups.id
        Setup(id, new.appPackage, new.appName, new.dailyLimitMinutes, new.alertMode, new.alertIntervalMinutes)
    }

    override suspend fun update(id: Int, new: NewSetup): Setup? = suspendTransaction(db) {
        val updatedRows = Setups.update({ Setups.id eq id }) {
            it[appPackage] = new.appPackage
            it[appName] = new.appName
            it[dailyLimitMinutes] = new.dailyLimitMinutes
            it[alertMode] = new.alertMode
            it[alertIntervalMinutes] = new.alertIntervalMinutes
        }
        if (updatedRows == 0) null
        else Setup(id, new.appPackage, new.appName, new.dailyLimitMinutes, new.alertMode, new.alertIntervalMinutes)
    }

    override suspend fun remove(id: Int): Boolean = suspendTransaction(db) {
        Setups.deleteWhere { Setups.id eq id } > 0
    }

    private fun ResultRow.toSetup() = Setup(
        this[Setups.id],
        this[Setups.appPackage],
        this[Setups.appName],
        this[Setups.dailyLimitMinutes],
        this[Setups.alertMode],
        this[Setups.alertIntervalMinutes],
    )
}

class PostgresSessionRepository(private val db: Database) : SessionRepository {

    override suspend fun list(setupId: Int): List<Session> = suspendTransaction(db) {
        Sessions.selectAll().where { Sessions.setupId eq setupId }
            .orderBy(Sessions.startedAt to SortOrder.ASC).map { it.toSession() }
    }

    override suspend fun find(setupId: Int, id: Int): Session? = suspendTransaction(db) {
        Sessions.selectAll().where { (Sessions.id eq id) and (Sessions.setupId eq setupId) }
            .singleOrNull()?.toSession()
    }

    override suspend fun add(setupId: Int, new: NewSession): Session = suspendTransaction(db) {
        val id = Sessions.insert {
            it[Sessions.setupId] = setupId
            it[startedAt] = new.startedAt.toOffsetDateTime()
            it[durationMinutes] = new.durationMinutes
        } get Sessions.id
        Session(id, setupId, new.startedAt, new.durationMinutes)
    }

    override suspend fun update(setupId: Int, id: Int, new: NewSession): Session? = suspendTransaction(db) {
        val updatedRows = Sessions.update({ (Sessions.id eq id) and (Sessions.setupId eq setupId) }) {
            it[startedAt] = new.startedAt.toOffsetDateTime()
            it[durationMinutes] = new.durationMinutes
        }
        if (updatedRows == 0) null
        else Session(id, setupId, new.startedAt, new.durationMinutes)
    }

    override suspend fun remove(setupId: Int, id: Int): Boolean = suspendTransaction(db) {
        Sessions.deleteWhere { (Sessions.id eq id) and (Sessions.setupId eq setupId) } > 0
    }

    private fun Instant.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.ofInstant(toJavaInstant(), ZoneOffset.UTC)

    private fun ResultRow.toSession() = Session(
        this[Sessions.id],
        this[Sessions.setupId],
        this[Sessions.startedAt].toInstant().toKotlinInstant(),
        this[Sessions.durationMinutes],
    )
}
