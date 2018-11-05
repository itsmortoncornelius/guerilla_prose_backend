import dao.Storage
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.locations.Locations
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import model.FileInfo
import model.GuerillaProse
import model.User
import org.koin.ktor.ext.inject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.DateFormat.FULL
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.*

object Server {
    fun build(): NettyApplicationEngine {
        return embeddedServer(Netty, 8080) {
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

                route("/guerillaProse") {

                    get {
                        try {
                            val guerillaProseList = storage.getGuerillaProses()
                            call.respond(HttpStatusCode.OK, guerillaProseList)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "error while getting guerilla prose data")
                        }
                    }

                    get("/{id}") {
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

                    post {
                        try {
                            val guerillaProse = call.receive<GuerillaProse>()
                            val createdGuerillaProse = storage.createGuerillaProse(guerillaProse)
                            call.respond(HttpStatusCode.OK, createdGuerillaProse)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "error while saving guerilla prose data")
                        }
                    }
                }

                route("/user") {

                    get {
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

                    post {
                        try {
                            val user = call.receive<User>()
                            if (user.email?.isNotBlank() == true
                                    && storage.getUser(user.email) != null) {
                                call.respond(HttpStatusCode.Conflict, user)
                                return@post
                            }
                            val createdUser = storage.createUser(
                                    if (user.isEmpty()) {
                                        val date = LocalDateTime.now()
                                        User(
                                                user.id,
                                                "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}i",
                                                "${date.nano}-Guest",
                                                ""
                                        )
                                    } else {
                                        user
                                    }
                            )
                            call.respond(HttpStatusCode.OK, createdUser)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "error while saving user data")
                        }
                    }

                    put {
                        try {
                            val user = call.receive<User>()
                            val updatedUser = storage.updateUser(user)
                            call.respond(HttpStatusCode.OK, updatedUser)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "error while saving user data")
                        }
                    }

                    delete {
                        try {
                            val userId = call.parameters["id"]?.toInt()
                            if (userId != null) {
                                val user = storage.getUser(userId)
                                storage.deleteUser(userId)
                                if (user != null) {
                                    call.respond(HttpStatusCode.OK, user)
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "the user was not found and could not be deleted")
                                }
                            } else {
                                call.respond(HttpStatusCode.InternalServerError, "did you set the correct parameter for Id?. It should be like user?id={id}")
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "error while saving user data")
                        }
                    }
                }

                post("/file") {
                    try {
                        val multipart = call.receiveMultipart()
                        var title = ""
                        var imageFile: File? = null
                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    if (part.name == "title") {
                                        title = part.value
                                    }
                                }
                                is PartData.FileItem -> {
                                    val ext = File(part.originalFileName).extension
                                    val directory = File("files")
                                    directory.mkdirs()
                                    val file = File(directory, "/upload-${System.currentTimeMillis()}-${title.hashCode()}.$ext")
                                    file.createNewFile()
                                    part.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
                                    imageFile = file
                                }
                            }

                            part.dispose()
                        }

                        if (imageFile?.path?.isNotBlank() == true) {
                            val fileInfo = FileInfo(imageFile!!.path)
                            call.respond(HttpStatusCode.Created, fileInfo)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "The file could not be correctly stored on the server")
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "The file could not be correctly stored on the server")
                    }
                }

                static("files") {
                    files("files")
                }
            }
        }
    }

    suspend fun InputStream.copyToSuspend(
            out: OutputStream,
            bufferSize: Int = DEFAULT_BUFFER_SIZE,
            yieldSize: Int = 4 * 1024 * 1024,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Long {
        return withContext(dispatcher) {
            val buffer = ByteArray(bufferSize)
            var bytesCopied = 0L
            var bytesAfterYield = 0L
            while (true) {
                val bytes = read(buffer).takeIf { it >= 0 } ?: break
                out.write(buffer, 0, bytes)
                if (bytesAfterYield >= yieldSize) {
                    yield()
                    bytesAfterYield %= yieldSize
                }
                bytesCopied += bytes
                bytesAfterYield += bytes
            }
            return@withContext bytesCopied
        }
    }
}