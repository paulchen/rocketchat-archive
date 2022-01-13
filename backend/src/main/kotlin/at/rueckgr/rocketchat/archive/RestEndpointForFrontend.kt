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

class RestEndpointForFrontend : Logging {
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
                        mongoOperation(this) {
                            result { mapOf("users" to loadUsers()) }
                        }
                    }
                }
                route("/channels") {
                    get {
                        mongoOperation(this) {
                            result { mapOf("channels" to loadChannels()) }
                        }
                    }
                }
                route("/channels/{channel}/messages/{message}") {
                    get {
                        mongoOperation(this) {
                            result {
                                val channel = call.parameters["channel"] ?: throw MongoOperationException("Missing channel", status = HttpStatusCode.BadRequest)
                                val message = call.parameters["message"] ?: throw MongoOperationException("Missing message", status = HttpStatusCode.BadRequest)

                                val dbMessage = Mongo.getInstance().getDatabase()
                                    .getCollection<RocketchatMessage>("rocketchat_message")
                                    .findOneById(message) ?: throw MongoOperationException("Not found", status = HttpStatusCode.NotFound)
                                if (dbMessage.t != null) {
                                    throw MongoOperationException("Not found", status = HttpStatusCode.NotFound)
                                }
                                val timestamp = dbMessage.ts

                                val filterConditions = and(
                                    RocketchatMessage::rid eq channel,
                                    RocketchatMessage::ts gt timestamp,
                                    RocketchatMessage::t eq null
                                )

                                val count = Mongo.getInstance().getDatabase()
                                    .getCollection<RocketchatMessage>("rocketchat_message")
                                    .find(filterConditions)
                                    .count()

                                val page = ceil((count + 1) / 100.0).toInt()

                                mapOf("channel" to channel, "message" to message, "page" to page)
                            }
                        }
                    }
                }
                route("/channels/{id}/messages") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "id"; required = true }
                                queryParameter { name = "sort"; required = true }
                            }
                            result {
                                val page = try {
                                    call.request.queryParameters["page"]?.toInt() ?: 1
                                }
                                catch (e: NumberFormatException) {
                                    throw MongoOperationException("Invalid value for page parameter", status = HttpStatusCode.BadRequest)
                                }
                                val limit = try {
                                    call.request.queryParameters["limit"]?.toInt() ?: 100
                                }
                                catch (e: NumberFormatException) {
                                    throw MongoOperationException("Invalid value for page parameter", status = HttpStatusCode.BadRequest)
                                }
                                val sortAscending = when(parameter("sort")) {
                                    "asc" -> true
                                    "desc" -> false
                                    else -> throw MongoOperationException("Invalid value for sort parameter", status = HttpStatusCode.BadRequest)
                                }

                                val filterConditions = mutableListOf(RocketchatMessage::rid eq parameter("id"), RocketchatMessage::t eq null)
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
                                        throw MongoOperationException("Invalid regular expression", status = HttpStatusCode.BadRequest)
                                    }
                                }

                                val messages = if (sortAscending) {
                                    Mongo.getInstance().getDatabase()
                                        .getCollection<RocketchatMessage>("rocketchat_message")
                                        .find(and(filterConditions))
                                        .ascendingSort(RocketchatMessage::ts)
                                }
                                else {
                                    Mongo.getInstance().getDatabase()
                                        .getCollection<RocketchatMessage>("rocketchat_message")
                                        .find(and(filterConditions))
                                        .descendingSort(RocketchatMessage::ts)

                                }
                                    .skip((page - 1) * limit)
                                    .limit(limit)
                                    .map { mapMessage(it) }
                                val messageCount = Mongo.getInstance().getDatabase()
                                    .getCollection<RocketchatMessage>("rocketchat_message")
                                    .find(and(filterConditions))
                                    .count()
                                mapOf("messages" to messages, "messageCount" to messageCount)
                            }
                        }
                    }
                }
                route("/reports") {
                    get {
                        mongoOperation(this) {
                            result {
                                val page = try {
                                    call.request.queryParameters["page"]?.toInt() ?: 1
                                }
                                catch (e: NumberFormatException) {
                                    throw MongoOperationException("Invalid value for page parameter", status = HttpStatusCode.BadRequest)
                                }
                                val limit = try {
                                    call.request.queryParameters["limit"]?.toInt() ?: 100
                                }
                                catch (e: NumberFormatException) {
                                    throw MongoOperationException("Invalid value for page parameter", status = HttpStatusCode.BadRequest)
                                }
                                val sortAscending = when(call.request.queryParameters["sort"]) {
                                    "asc" -> true
                                    "desc" -> false
                                    else -> throw MongoOperationException("Invalid value for sort parameter", status = HttpStatusCode.BadRequest)
                                }

                                val database = Mongo.getInstance().getDatabase()
                                val users = loadUsers().associateBy(User::id)
                                val channels = loadChannels().map { it.id }
                                val reports = if (sortAscending) {
                                    database
                                        .getCollection<RocketchatReport>("rocketchat_reports")
                                        .find()
                                        .ascendingSort(RocketchatReport::ts)
                                }
                                else {
                                    database
                                        .getCollection<RocketchatReport>("rocketchat_reports")
                                        .find()
                                        .descendingSort(RocketchatReport::ts)
                                }
                                    .skip((page - 1) * limit)
                                    .limit(limit)
                                    .filter { channels.contains(it.message.rid) }
                                    .map { mapReport(it, users) }

                                val reportsCount = database
                                    .getCollection<RocketchatReport>("rocketchat_reports")
                                    .find()
                                    .count()
                                mapOf("reports" to reports, "reportsCount" to reportsCount)
                            }
                        }
                    }
                }
                route("/channels/{id}/stats") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "id"; required = true }
                            }
                            result {
                                val id = parameter("id")

                                val database = Mongo.getInstance().getDatabase()

                                val users = database.getCollection<RocketchatUser>("users")
                                    .find()
                                    .associate { it._id to it.username }

                                val userMessageCount = database
                                    .getCollection<RocketchatMessage>("rocketchat_message")
                                    .aggregate<StatsResult>(
                                        match(RocketchatMessage::rid eq id, RocketchatMessage::t eq null),
                                        project(
                                            StatsResult::key from RocketchatMessage::u / UserData::_id,
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
                                    .map { MessageCount(users[it.key]!!, it.value.toInt()) }

                                val messagesPerMonth = database
                                    .getCollection<RocketchatMessage>("rocketchat_message")
                                    .aggregate<StatsResult>(
                                        match(RocketchatMessage::rid eq id, RocketchatMessage::t eq null),
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
                                        match(RocketchatMessage::rid eq id, RocketchatMessage::t eq null),
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
                                        match(RocketchatMessage::rid eq id, RocketchatMessage::t eq null),
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

                                ChannelStats(
                                    userMessageCount,
                                    mapOf(
                                        "messagesPerMonth" to TimebasedMessageCount(messagesPerMonth),
                                        "messagesPerYear" to TimebasedMessageCount(messagesPerYear),
                                        "topDays" to TimebasedMessageCount(topDays)
                                    )
                                )
                            }
                        }
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

    private fun mapMessage(message: RocketchatMessage) =
        Message(message._id, message.msg, message.ts, message.u.username)

    private fun mapReport(report: RocketchatReport, users: Map<String, User>) =
        Report(report._id, mapMessage(report.message), report.description, report.ts, users[report.userId]!!)

    private fun mapUser(user: RocketchatUser) = User(user._id, user.name, user.username)

    private fun mapChannel(channel: RocketchatRoom) = Channel(channel.name!!, channel._id)

    private fun loadUsers() = Mongo.getInstance().getDatabase()
        .getCollection<RocketchatUser>("users")
        .find()
        .map { mapUser(it) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username })

    private fun loadChannels() = Mongo.getInstance().getDatabase()
        .getCollection<RocketchatRoom>("rocketchat_room")
        .find()
        .filter { it.t == "c" }
        .map { mapChannel(it) }
}
