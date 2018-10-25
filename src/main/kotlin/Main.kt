import com.google.gson.Gson
import dao.Storage
import di.DependencyProvider
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import model.GuerillaProse
import model.User
import org.koin.core.Koin
import org.koin.ktor.ext.inject
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin

fun main(args: Array<String>) {
    Koin.logger = PrintLogger()
    startKoin(listOf(DependencyProvider.mainModule))

    val server = embeddedServer(Netty, 8080) {
        install(DefaultHeaders)
        install(CallLogging)
        install(Locations)
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }

        routing {
            val storage: Storage by inject()
            val gson: Gson by inject()

            get("/guerillaProse") {
                try {
                    val guerillaProseList = storage.getGuerillaProses()
                    call.respond(HttpStatusCode.OK, guerillaProseList)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "error while getting guerilla prose data")
                }
            }

            get("/guerillaProse/{id}") {
                try {
                    val guerillaProseId = call.parameters["id"]?.toInt()
                    val guerillaProse = guerillaProseId?.let { id -> storage.getGuerillaProse(id) }
                    if (guerillaProse != null) {
                        call.respond(HttpStatusCode.OK, guerillaProse)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "The resource cannot be found in the database. Make sure you sent the correct id")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "error while getting guerilla prose data")
                }
            }

            post("/guerillaProse") {
                try {
                    val guerillaProse = call.receive<GuerillaProse>()
                    val createdGuerillaProse = storage.createGuerillaProse(guerillaProse)
                    call.respond(HttpStatusCode.OK, createdGuerillaProse)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "error while saving guerilla prose data")
                }
            }

            get("/user/{id}") {
                try {
                    val userId = call.parameters["id"]?.toInt()
                    val user = userId?.let { id -> storage.getUser(id) }
                    if (user != null) {
                        call.respond(HttpStatusCode.OK, user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "The resource cannot be found in the database. Make sure you sent the correct id")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "error while getting user data")
                }
            }

            post("/user") {
                try {
                    val user = call.receive<User>()
                    if (user.email?.isNotBlank() == true) {
                        val existingUser = storage.getUser(user.email)
                        if (existingUser != null) {
                            call.respond(HttpStatusCode.Conflict, existingUser)
                            return@post
                        }
                    }
                    storage.createUser(user)
                    call.respond(HttpStatusCode.OK, user)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "error while saving user data")
                }
            }
        }
    }
    server.start(wait = true)
}
