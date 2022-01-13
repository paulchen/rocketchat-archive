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

class RestEndpointForBot(private val ravusBotService: RavusBotService) {
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
                        mongoOperation(this) {
                            result {
                                val username = call.parameters["username"] ?: throw MongoOperationException("Missing channel", status = HttpStatusCode.BadRequest)
                                val usernamesFromRavusBot = ravusBotService.getUsernames(username)

                                val database = Mongo.getInstance().getDatabase()

                                val usernames = usernamesFromRavusBot.ifEmpty { listOf(username) }.map { it.lowercase() }
                                val databaseUsers = database.getCollection<RocketchatUser>("users")
                                    .find()
                                    .map { User(it._id, it.name, it.username) }
                                    .filter { usernames.contains(it.name.lowercase()) || usernames.contains(it.username.lowercase()) }
                                if (databaseUsers.isEmpty()) {
                                    throw MongoOperationException("Unknown username", status = HttpStatusCode.NotFound)
                                }
                                val databaseUser = databaseUsers[0]

                                val message = database
                                    .getCollection<RocketchatMessage>("rocketchat_message")
                                    .find(
                                        and(
                                            RocketchatMessage::u / UserData::_id eq databaseUser.id,
                                            RocketchatMessage::t eq null
                                        )
                                    )
                                    .descendingSort(RocketchatMessage::ts)
                                    .limit(1)
                                    .singleOrNull()

                                val userDetails = UserDetails(databaseUser.username, message?.ts)

                                mapOf("user" to userDetails)
                            }
                        }
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
