package dao

import model.User
import org.jetbrains.squash.definition.*

object GuerillaProseDao : TableDefinition() {
    val id = integer("id").autoIncrement().primaryKey()
    val text = varchar("text", length = 333)
    val imageUrl = varchar("imageUrl", 64)
    val label = varchar("label", 64)
    val userId = reference(UserDao.id, "userId")
    val date = date("date")
}

object UserDao : TableDefinition() {
    val id = integer("id").autoIncrement().primaryKey()
    val firstname = varchar("firstname", length = 128)
    val lastname = varchar("lastname", length = 128)
    val email = varchar("email", length = 128)
}