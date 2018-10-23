package backend

import com.google.gson.Gson
import dao.GuerillaProseStorage
import di.DependencyProvider
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import model.GuerillaProse
import org.koin.core.Koin
import org.koin.ktor.ext.inject
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin

class GuerillaProseApp {
    fun main(args: Array<String>) {

    }
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
                    val storage: GuerillaProseStorage by inject()
                    val gson: Gson by inject()

                    get("/guerillaProse") {
                        try {
                            val guerillaProseList = storage.getGuerillaProses()
                            val jsonResponse = gson.toJson(guerillaProseList)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while getting data"))
                        }
                    }

                    get("/guerillaProse/{id}") {
                        try {
                            val guerillaProseId = call.parameters["id"]?.toInt()
                            val guerillaProse = guerillaProseId?.let { id -> storage.getGuerillaProse(id) }
                            val jsonResponse = gson.toJson(guerillaProse)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while getting data"))
                        }
                    }

                    post("/guerillaProse") {
                        try {
                            val guerillaProse = call.receive<GuerillaProse>()
                            storage.createGuerillaProse(guerillaProse)
                            val jsonResponse = gson.toJson(guerillaProse)
                            call.respondText(jsonResponse, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(gson.toJson("error while saving data"))
                        }
                    }
                }
            }
            server.start(wait = true)
        }
    }
}
