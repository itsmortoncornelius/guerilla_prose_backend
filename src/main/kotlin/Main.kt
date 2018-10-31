import di.DependencyProvider
import org.koin.core.Koin
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin

fun main(args: Array<String>) {
    Koin.logger = PrintLogger()
    startKoin(listOf(DependencyProvider.mainModule))

    val server = Server.build()
    server.start(wait = true)
}