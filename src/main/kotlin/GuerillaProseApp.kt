import com.google.gson.Gson
import di.DependencyProvider
import grapgql.AppSchema
import grapgql.graphql
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.content.default
import io.ktor.http.content.static
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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
                    val appSchema: AppSchema by inject()
                    val gson: Gson by inject()

                    graphql(log, gson, appSchema.schema)

                    static("/") {
                        default("index.html")
                    }
                }
            }
            server.start(wait = true)
        }
    }
}
