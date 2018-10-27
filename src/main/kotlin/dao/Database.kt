package dao

import model.GuerillaProse
import model.User
import org.jetbrains.squash.connection.DatabaseConnection
import org.jetbrains.squash.connection.transaction
import org.jetbrains.squash.dialects.h2.H2Connection
import org.jetbrains.squash.expressions.eq
import org.jetbrains.squash.graph.id
import org.jetbrains.squash.query.*
import org.jetbrains.squash.results.ResultRow
import org.jetbrains.squash.results.get
import org.jetbrains.squash.schema.create
import org.jetbrains.squash.statements.*
import java.sql.Timestamp

fun ResultRow.toUser() = User(
        id = this[UserDao.id],
        firstname = this[UserDao.firstname],
        lastname = this[UserDao.lastname],
        email = this[UserDao.email]
)

fun ResultRow.toGuerillaProse() = GuerillaProse(
        id = this[GuerillaProseDao.id],
        text = this[GuerillaProseDao.text],
        imageUrl = this[GuerillaProseDao.imageUrl],
        label = this[GuerillaProseDao.label],
        date = this[GuerillaProseDao.date],
        userId = this[GuerillaProseDao.userId]
)

class Database(val db: DatabaseConnection = H2Connection.createMemoryConnection(catalogue = "DB_CLOSE_DELAY=-1")) : Storage {

    init {
        db.transaction {
            databaseSchema().create(GuerillaProseDao, UserDao)
        }
    }

    override fun createGuerillaProse(guerillaProse: GuerillaProse): GuerillaProse {
        val id = db.transaction {
            insertInto(GuerillaProseDao).values {
                it[text] = guerillaProse.text?.let { text -> text }
                it[imageUrl] = guerillaProse.imageUrl?.let { imageUrl -> imageUrl }
                it[label] = guerillaProse.label?.let { label -> label }
                it[userId] = guerillaProse.userId?.let { userId -> userId }
                it[date] = Timestamp(System.currentTimeMillis()).time
            }.fetch(GuerillaProseDao.id).execute()
        }

        guerillaProse.id = id
        return guerillaProse
    }

    override fun getGuerillaProses(): List<GuerillaProse> = db.transaction {
        from(GuerillaProseDao)
                .select()
                .orderBy(GuerillaProseDao.date, ascending = true)
                .execute()
                .map { it.toGuerillaProse() }
                .toList()
    }

    override fun getGuerillaProsesForLabel(label: String): List<GuerillaProse> = db.transaction {
        from(GuerillaProseDao)
                .select()
                .where(GuerillaProseDao.label eq label)
                .orderBy(GuerillaProseDao.date, ascending = true)
                .execute()
                .map { it.toGuerillaProse() }
                .toList()
    }

    override fun getGuerillaProsesForUser(userId: Int): List<GuerillaProse> = db.transaction {
        from(GuerillaProseDao)
                .select()
                .where(GuerillaProseDao.userId eq userId)
                .orderBy(GuerillaProseDao.date, ascending = true)
                .execute()
                .map { it.toGuerillaProse() }
                .toList()
    }

    override fun getGuerillaProse(id: Int): GuerillaProse? = db.transaction {
        val row = from(GuerillaProseDao).where { GuerillaProseDao.id eq id }.execute().singleOrNull()
        row?.toGuerillaProse()
    }

    override fun createUser(user: User): User {
        val id = db.transaction {
            insertInto(UserDao).values {
                it[firstname] = user.firstname
                it[lastname] = user.lastname
                it[email] = user.email
            }.fetch(UserDao.id).execute()
        }

        user.id = id
        return user
    }

    override fun getUser(userId: Int): User? = db.transaction {
        val row = from(UserDao).where { UserDao.id eq userId }.execute().singleOrNull()
        row?.toUser()
    }

    override fun getUser(email: String): User? = db.transaction {
        val row = from(UserDao).where { UserDao.email eq email }.execute().singleOrNull()
        row?.toUser()
    }

    override fun updateUser(user: User) = db.transaction {
        update(UserDao).where { UserDao.id eq user.id!! }.execute()
    }

    override fun deleteUser(id: Int) = db.transaction {
        deleteFrom(UserDao).where { UserDao.id eq id }.execute()
    }

    override fun close() {
        db.close()
    }
}