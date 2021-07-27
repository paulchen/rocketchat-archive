package at.rueckgr.rocketchat.archive

import com.fasterxml.jackson.databind.SerializationFeature
import com.mongodb.client.model.Filters
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.*
import java.time.ZonedDateTime
import kotlin.math.ceil

data class RocketchatRoom(val _id: String, val t: String, val name: String?)

data class UserData(val _id: String, val username: String, val name: String?)

data class RocketchatMessage(val _id: String, val rid: String, val msg: String, val ts: ZonedDateTime, val u: UserData)

data class RocketchatUser(val _id: String, val name: String, val username: String)

data class Channel(val name: String, val id: String)

data class Message(val id: String, val message: String, val timestamp: ZonedDateTime, val username: String)

data class User(val id: String, val username: String)

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
            route("/users") {
                get {
                    val client = KMongo.createClient("mongodb://mongo:27017")
                    val database = client.getDatabase("rocketchat")
                    val users = database.getCollection<RocketchatUser>("users")
                        .find()
                        .map { User(it._id, it.username) }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username })
                    call.respond(mapOf("users" to users))
                    client.close()
                }
            }
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
                    val limit = call.request.queryParameters["limit"]?.toInt() ?: 100
                    val sortAscending = call.request.queryParameters["sort"] == "asc"
                    val id = call.parameters["id"] ?: return@get call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                    val client = KMongo.createClient("mongodb://mongo:27017")
                    val database = client.getDatabase("rocketchat")

                    val filterConditions = mutableListOf(RocketchatMessage::rid eq id)
                    val userIds = call.parameters["userIds"]?.trim()?.split(",") ?: emptyList()
                    if (userIds.isNotEmpty() && !(userIds.size == 1 && userIds.first().isBlank())) {
                        filterConditions.add(RocketchatMessage::u / UserData::_id `in` userIds)
                    }
                    val text = call.parameters["text"]?.trim() ?: ""
                    if (text.isNotBlank()) {
                        filterConditions.add(Filters.regex("msg", text, "i"))
                    }
                    val filterCondition = when (filterConditions.size) {
                        1 -> filterConditions.first()
                        else -> and(filterConditions)
                    }

                    val messages = if (sortAscending) {
                        database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .find(filterCondition)
                            .ascendingSort(RocketchatMessage::ts)
                    }
                    else {
                        database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .find(filterCondition)
                            .descendingSort(RocketchatMessage::ts)

                    }
                        .skip((page - 1) * limit)
                        .limit(limit)
                        .map { Message(it._id, it.msg, it.ts, it.u.username) }
                    val messageCount = database
                        .getCollection<RocketchatMessage>("rocketchat_message")
                        .find(filterCondition)
                        .count()
                    call.respond(mapOf("messages" to messages, "messageCount" to messageCount))
                    client.close()
                }
            }
        }
    }.start(wait = true)
}
