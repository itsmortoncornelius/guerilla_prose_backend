package grapgql

import com.github.pgutkowski.kgraphql.KGraphQL
import dao.GuerillaProseStorage
import model.GuerillaProse
import model.User
import java.time.LocalDate

class AppSchema(private val storage: GuerillaProseStorage) {

    val schema = KGraphQL.schema {

        configure {
            useDefaultPrettyPrinter = true
        }

        stringScalar<LocalDate> {
            serialize = { date -> date.toString() }
            deserialize = { dateString -> LocalDate.parse(dateString) }
        }

        query("guerillaproses") {
            description = "Returns all available guerilla prose"
            resolver(storage::getGuerillaProses)
        }

        query("guerillaprosesForLabel") {
            description = "Returns all available guerilla prose for one label"
            resolver { label: String -> storage.getGuerillaProsesForLabel(label) }
        }

        query("guerillaprosesForUser") {
            description = "Returns all available guerilla prose for one user"
            resolver { userId: Int -> storage.getGuerillaProsesForUser(userId) }
        }

        query("guerillaprose") {
            description = "Returns the guerilla prose specified by the id"
            resolver { id: Int -> storage.getGuerillaProse(id) ?: throw Throwable("guerilla prose with id: $id does not exist")  }
        }

        query("user") {
            description = "Returns the user specified by the id"
            resolver { id: Int -> storage.getUser(id) ?: throw Throwable("User with id: $id does not exist") }
        }

        mutation("createUser") {
            description = "Adds a new user to the database"
            resolver { user: User -> storage.createUser(user) }
        }

        mutation("createGuerillaProse") {
            description = "Adds a new guerilla prose to the database"
            resolver { guerillaprose: GuerillaProse -> storage.createGuerillaProse(guerillaprose) }
        }

        type<GuerillaProse> {
            description = "A guerilla prose - a combination of a picture and a 333 character long text"
            property(GuerillaProse::imageUrl) {
                description = "The image of the guerilla prose"
            }
            property(GuerillaProse::text) {
                description = "The text of the guerilla prose"
            }
        }

        type<User> {
            description = "A user object"
            property(User::email) {
                description = "The email of the user"
            }
        }
    }

}