package dao

import org.jetbrains.squash.definition.*

object GuerillaProseDao : TableDefinition() {
    val id = integer("id").autoIncrement().primaryKey()
    var text = varchar("text", length = 333)
    var imageUrl = varchar("imageUrl", 255)
    var label = varchar("label", 128)
    val userId = reference(UserDao.id, "userId")
    val date = long("date")
}

object UserDao : TableDefinition() {
    val id = integer("id").autoIncrement().primaryKey()
    var firstname = varchar("firstname", length = 128)
    var lastname = varchar("lastname", length = 128)
    var email = varchar("email", length = 128)
}