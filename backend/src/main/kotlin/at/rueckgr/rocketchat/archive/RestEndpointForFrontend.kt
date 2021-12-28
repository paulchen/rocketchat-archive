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
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.ceil

class RestEndpointForFrontend(private val archiveConfiguration: ArchiveConfiguration) : Logging {
    fun start() {
        embeddedServer(Netty, 8080) {
            install(ContentNegotiation) {
                jackson {
                    findAndRegisterModules()
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            routing {
                route("/users") {
                    get {
                        val client = KMongo.createClient(archiveConfiguration.mongoUrl)
                        val database = client.getDatabase(archiveConfiguration.database)
                        val users = database.getCollection<RocketchatUser>("users")
                            .find()
                            .map { User(it._id, it.name, it.username) }
                            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username })
                        call.respond(mapOf("users" to users))
                        client.close()
                    }
                }
                route("/channels") {
                    get {
                        val client = KMongo.createClient(archiveConfiguration.mongoUrl)
                        val database = client.getDatabase(archiveConfiguration.database)
                        val channels = database.getCollection<RocketchatRoom>("rocketchat_room")
                            .find()
                            .filter { it.t == "c" }
                            .map { Channel(it.name!!, it._id) }
                        call.respond(mapOf("channels" to channels))
                        client.close()
                    }
                }
                route("/channels/{channel}/messages/{message}") {
                    get {
                        val channel = call.parameters["channel"] ?: return@get call.respondText("Missing channel", status = HttpStatusCode.BadRequest)
                        val message = call.parameters["message"] ?: return@get call.respondText("Missing message", status = HttpStatusCode.BadRequest)

                        val client = KMongo.createClient(archiveConfiguration.mongoUrl)
                        val database = client.getDatabase(archiveConfiguration.database)

                        val dbMessage = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .findOneById(message) ?: return@get call.respondText("Not found", status = HttpStatusCode.NotFound)
                        val timestamp = dbMessage.ts

                        val filterConditions = and(
                            RocketchatMessage::rid eq channel,
                            RocketchatMessage::ts gt timestamp,
                            RocketchatMessage::t eq null
                        )

                        val count = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .find(filterConditions)
                            .count()

                        val page = ceil((count + 1) / 100.0).toInt()

                        call.respond(mapOf("channel" to channel, "message" to message, "page" to page))
                        client.close()
                    }
                }
                route("/channels/{id}/messages") {
                    get {
                        val page = try {
                            call.request.queryParameters["page"]?.toInt() ?: 1
                        }
                        catch (e: NumberFormatException) {
                            return@get call.respondText("Invalid value for page parameter", status = HttpStatusCode.BadRequest)
                        }
                        val limit = try {
                            call.request.queryParameters["limit"]?.toInt() ?: 100
                        }
                        catch (e: NumberFormatException) {
                            return@get call.respondText("Invalid value for page parameter", status = HttpStatusCode.BadRequest)
                        }
                        val sortAscending = when(call.request.queryParameters["sort"]) {
                            "asc" -> true
                            "desc" -> false
                            else -> return@get call.respondText("Invalid value for sort parameter", status = HttpStatusCode.BadRequest)
                        }
                        val id = call.parameters["id"] ?: return@get call.respondText("Missing channel", status = HttpStatusCode.BadRequest)
                        val client = KMongo.createClient(archiveConfiguration.mongoUrl)
                        val database = client.getDatabase(archiveConfiguration.database)

                        val filterConditions = mutableListOf(RocketchatMessage::rid eq id, RocketchatMessage::t eq null)
                        val userIds = call.parameters["userIds"]?.trim()?.split(",") ?: emptyList()
                        if (userIds.isNotEmpty() && !(userIds.size == 1 && userIds.first().isBlank())) {
                            filterConditions.add(RocketchatMessage::u / UserData::_id `in` userIds)
                        }
                        val text = call.parameters["text"]?.trim() ?: ""
                        if (text.isNotBlank()) {
                            try {
                                filterConditions.add(Filters.regex("msg", Pattern.compile(text, Pattern.CASE_INSENSITIVE)))
                            }
                            catch (e: PatternSyntaxException) {
                                logger().info("Invalid regular expression in request: {}", text)
                                return@get call.respondText("Invalid regular expression", status = HttpStatusCode.BadRequest)
                            }
                        }

                        val messages = if (sortAscending) {
                            database
                                .getCollection<RocketchatMessage>("rocketchat_message")
                                .find(and(filterConditions))
                                .ascendingSort(RocketchatMessage::ts)
                        }
                        else {
                            database
                                .getCollection<RocketchatMessage>("rocketchat_message")
                                .find(and(filterConditions))
                                .descendingSort(RocketchatMessage::ts)

                        }
                            .skip((page - 1) * limit)
                            .limit(limit)
                            .map { Message(it._id, it.msg, it.ts, it.u.username) }
                        val messageCount = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .find(and(filterConditions))
                            .count()
                        call.respond(mapOf("messages" to messages, "messageCount" to messageCount))
                        client.close()
                    }
                }
                route("/channels/{id}/stats") {
                    get {
                        val id = call.parameters["id"] ?: return@get call.respondText("Missing channel", status = HttpStatusCode.BadRequest)

                        val client = KMongo.createClient(archiveConfiguration.mongoUrl)
                        val database = client.getDatabase(archiveConfiguration.database)

                        val userMessageCount = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .aggregate<StatsResult>(
                                match(RocketchatMessage::rid eq id),
                                project(
                                    StatsResult::key from RocketchatMessage::u / UserData::username,
                                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                                ),
                                group(
                                    StatsResult::key,
                                    StatsResult::key first StatsResult::key,
                                    StatsResult::value sum StatsResult::value
                                ),
                                sort(
                                    descending(StatsResult::value)
                                )
                            )
                            .toList()
                            .map { MessageCount(it.key, it.value.toInt()) }

                        val messagesPerMonth = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .aggregate<StatsResult>(
                                match(RocketchatMessage::rid eq id),
                                project(
                                    StatsResult::key from month(RocketchatMessage::ts),
                                    StatsResult::additionalKey1 from year(RocketchatMessage::ts),
                                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                                ),
                                group(
                                    fields(StatsResult::key from StatsResult::key, StatsResult::additionalKey1 from StatsResult::additionalKey1),
                                    StatsResult::key first StatsResult::key,
                                    StatsResult::additionalKey1 first StatsResult::additionalKey1,
                                    StatsResult::value sum 1
                                )
                            )
                            .toList()
                            .sortedBy { it.additionalKey1 + "-" + String.format("%02d", it.key.toInt()) }
                            .map { MessageCount(it.additionalKey1 + "-" + String.format("%02d", it.key.toInt()), it.value.toInt()) }

                        val messagesPerYear = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .aggregate<StatsResult>(
                                match(RocketchatMessage::rid eq id),
                                project(
                                    StatsResult::key from year(RocketchatMessage::ts),
                                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                                ),
                                group(
                                    StatsResult::key,
                                    StatsResult::key first StatsResult::key,
                                    StatsResult::value sum StatsResult::value
                                ),
                                sort(
                                    ascending(StatsResult::key)
                                )
                            )
                            .toList()
                            .map { MessageCount(it.key, it.value.toInt()) }

                        val topDays = database
                            .getCollection<RocketchatMessage>("rocketchat_message")
                            .aggregate<StatsResult>(
                                match(RocketchatMessage::rid eq id),
                                project(
                                    StatsResult::key from dayOfMonth(RocketchatMessage::ts),
                                    StatsResult::additionalKey1 from month(RocketchatMessage::ts),
                                    StatsResult::additionalKey2 from year(RocketchatMessage::ts),
                                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                                ),
                                group(
                                    fields(
                                        StatsResult::key from StatsResult::key,
                                        StatsResult::additionalKey1 from StatsResult::additionalKey1,
                                        StatsResult::additionalKey2 from StatsResult::additionalKey2
                                    ),
                                    StatsResult::key first StatsResult::key,
                                    StatsResult::additionalKey1 first StatsResult::additionalKey1,
                                    StatsResult::additionalKey2 first StatsResult::additionalKey2,
                                    StatsResult::value sum 1
                                ),
                                sort(
                                    descending(StatsResult::value)
                                ),
                                limit(10)
                            )
                            .toList()
                            .map {
                                MessageCount(
                                    it.additionalKey2 + "-" + String.format("%02d", it.additionalKey1!!.toInt()) + "-" + String.format("%02d", it.key.toInt()),
                                    it.value.toInt()
                                )
                            }

                        call.respond(
                            ChannelStats(
                                userMessageCount,
                                mapOf(
                                    "messagesPerMonth" to TimebasedMessageCount(messagesPerMonth),
                                    "messagesPerYear" to TimebasedMessageCount(messagesPerYear),
                                    "topDays" to TimebasedMessageCount(topDays)
                                )
                            )
                        )
                        client.close()
                    }
                }
                route("/version") {
                    get {
                        call.respond(mapOf("version" to VersionHelper.instance.getVersion().revision))
                    }
                }
            }
        }.start()
    }
}
