package backend

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
import io.ktor.locations.Locations
import io.ktor.request.receive
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

class GuerillaProseApp {
    companion object {
        fun main() {
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
                            val jsonResponse = gson.toJson(guerillaProseList)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while getting guerilla prose data"))
                        }
                    }

                    get("/guerillaProse/{id}") {
                        try {
                            val guerillaProseId = call.parameters["id"]?.toInt()
                            val guerillaProse = guerillaProseId?.let { id -> storage.getGuerillaProse(id) }
                            val jsonResponse = gson.toJson(guerillaProse)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while getting guerilla prose data"))
                        }
                    }

                    post("/guerillaProse") {
                        try {
                            val guerillaProse = call.receive<GuerillaProse>()
                            storage.createGuerillaProse(guerillaProse)
                            val jsonResponse = gson.toJson(guerillaProse)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while saving guerilla prose data"))
                        }
                    }

                    get("/user/{id}") {
                        try {
                            val userId = call.parameters["id"]?.toInt()
                            val user = userId?.let { id -> storage.getUser(id) }
                            val jsonResponse = gson.toJson(user)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while getting user data"))
                        }
                    }

                    post("/user") {
                        try {
                            val user = call.receive<User>()
                            if (user.email?.isNotBlank() == true) {
                                val existingUser = storage.getUser(user.email)
                                if (existingUser != null) {
                                    val jsonResponse = gson.toJson(existingUser)
                                    call.respondText(jsonResponse, ContentType.Application.Json)
                                    return@post
                                }
                            }
                            storage.createUser(user)
                            val jsonResponse = gson.toJson(user)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while saving user data"))
                        }
                    }
                }
            }
            server.start(wait = true)
        }
    }
}
