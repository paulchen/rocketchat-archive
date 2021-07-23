package at.rueckgr.rocketchat.archive

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
import java.time.LocalDateTime
import kotlin.math.ceil

data class RocketchatRoom(val _id: String, val t: String, val name: String?)

data class UserData(val _id: String, val username: String, val name: String?)

data class RocketchatMessage(val _id: String, val rid: String, val msg: String, val ts: LocalDateTime, val u: UserData)

data class RocketchatUser(val _id: String, val name: String, val username: String)

data class Channel(val name: String, val id: String)

data class Message(val id: String, val message: String, val timestamp: LocalDateTime, val username: String)

fun main() {
    /*
    val client = KMongo.createClient()
    val database = client.getDatabase("rocketchat")
    database.getCollection<RocketchatRoom>("rocketchat_room")
        .find()
        .filter { it.t == "c" }
        .forEach { println(it.name) }

    database
        .getCollection<RocketchatMessage>("rocketchat_message")
        .find(RocketchatMessage::rid eq "GENERAL")
        .descendingSort(RocketchatMessage::ts)
        .skip(10)
        .limit(10)
        .forEach { println(it.u.username + " " + it.msg) }

    database
        .getCollection<RocketchatUser>("users")
        .find()
        .forEach { println(it.username) }

     */

    embeddedServer(Netty, 8080) {
        install(ContentNegotiation) {
            jackson {
                findAndRegisterModules()
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            }
        }
        routing {
            route("/channels") {
                get {
                    val client = KMongo.createClient("mongodb://mongo:27017")
                    val database = client.getDatabase("rocketchat")
                    val channels = database.getCollection<RocketchatRoom>("rocketchat_room")
                        .find()
                        .filter { it.t == "c" }
                        .map { Channel(it.name!!, it._id) }
                    call.respond(mapOf("channels" to channels))
                    client.close()
                }
            }
            route("/channels/{id}/messages") {
                get {
                    val page = call.request.queryParameters["page"]?.toInt() ?: 1
                    val id = call.parameters["id"] ?: return@get call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                    val client = KMongo.createClient("mongodb://mongo:27017")
                    val database = client.getDatabase("rocketchat")
                    val messages = database
                        .getCollection<RocketchatMessage>("rocketchat_message")
                        .find(RocketchatMessage::rid eq id)
                        .descendingSort(RocketchatMessage::ts)
                        .skip((page - 1) * 100)
                        .limit(100)
                        .map { Message(it._id, it.msg, it.ts, it.u.username) }
                    val messageCount = database
                        .getCollection<RocketchatMessage>("rocketchat_message")
                        .find(RocketchatMessage::rid eq id)
                        .count()
                    val pageCount = ceil(messageCount.toDouble() / 100).toInt()
                    call.respond(mapOf("messages" to messages, "pages" to pageCount))
                    client.close()
                }
            }
        }
    }.start(wait = true)
}
