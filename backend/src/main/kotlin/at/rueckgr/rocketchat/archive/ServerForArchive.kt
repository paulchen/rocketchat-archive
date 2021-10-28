package at.rueckgr.rocketchat.archive

import at.rueckgr.rocketchat.ravusbot.RavusBotService
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.*

class ServerForArchive(private val archiveConfiguration: ArchiveConfiguration, private val ravusBotService: RavusBotService) {
    fun start() {
        embeddedServer(Netty, 8081) {
            install(ContentNegotiation) {
                jackson {
                    findAndRegisterModules()
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            routing {
                route("/user/{username}") {
                    get {
                        val username = call.parameters["username"] ?: return@get call.respondText("Missing channel", status = HttpStatusCode.BadRequest)
                        val usernamesFromRavusBot = ravusBotService.getUsernames(username)

                        val client = KMongo.createClient(archiveConfiguration.mongoUrl)
                        val database = client.getDatabase(archiveConfiguration.database)

                        val usernames = usernamesFromRavusBot.ifEmpty { listOf(username) }
                        val databaseUser = database.getCollection<RocketchatUser>("users")
                            .find(RocketchatUser::username `in` usernames)
                            .singleOrNull()
                            ?: return@get call.respondText("Unknown username", status = HttpStatusCode.NotFound)

                        val message = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .find(RocketchatMessage::u / UserData::username `in` usernames)
                            .descendingSort(RocketchatMessage::ts)
                            .limit(1)
                            .singleOrNull()

                        val userDetails = UserDetails(databaseUser.username, message?.ts)

                        call.respond(mapOf("user" to userDetails))
                        client.close()
                    }
                }
                route("/version") {
                    get {
                        call.respond(mapOf("version" to VersionHelper.instance.getVersion()))
                    }
                }
            }
        }.start()
    }
}
