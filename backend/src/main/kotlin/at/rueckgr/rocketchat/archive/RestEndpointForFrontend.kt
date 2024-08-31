package at.rueckgr.rocketchat.archive

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Suppress("ExtractKtorModule")
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
                            result { mapOf("users" to RocketchatDatabase().getUsers()) }
                        }
                    }
                }
                route("/channels") {
                    get {
                        mongoOperation(this) {
                            result { mapOf("channels" to RocketchatDatabase().getChannels()) }
                        }
                    }
                }
                route("/channels/{channel}/messages/{message}") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "channel"; required = true }
                                urlParameter { name = "message"; required = true }
                            }
                            result {
                                val message = parameter("message")!!
                                val channel = parameter("channel")!!
                                val page = RocketchatDatabase().getPageForMessage(message, channel)

                                mapOf(
                                    "channel" to parameter("channel"),
                                    "message" to parameter("message"),
                                    "page" to page
                                )
                            }
                        }
                    }
                }
                route("/channels/{channel}/messages/{message}/history") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "channel"; required = true }
                                urlParameter { name = "message"; required = true }
                            }
                            result {
                                val message = parameter("message")!!
                                val channel = parameter("channel")!!
                                val history = RocketchatDatabase().getMessageHistory(message, channel)

                                mapOf(
                                    "channel" to parameter("channel"),
                                    "message" to parameter("message"),
                                    "history" to history
                                )
                            }
                        }
                    }
                }
                route("/channels/{channel}/messages") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "channel"; required = true }
                                queryParameter { name = "userIds" }
                                queryParameter { name = "text" }
                                queryParameter { name = "date" }
                                queryParameter { name = "attachments" }
                                paginationParameters(this)
                            }
                            result {
                                val paginationParameters = getPaginationParameters(this@mongoOperation)
                                val channel = parameter("channel")!!
                                val userIds = parameter("userIds")?.trim()?.split(",") ?: emptyList()
                                val text = parameter("text")?.trim() ?: ""
                                val date = dateParameter("date")
                                val attachments = boolParameter("attachments") ?: false

                                val (messages, messageCount) =
                                    RocketchatDatabase().getMessages(channel, userIds, text, date, attachments, paginationParameters)

                                val processedMessages = messages
                                    .map { Message(it.id, it.rid, MessageProcessor.process(it.message), it.timestamp, it.username, it.attachments, it.editedAt, it.editedBy) }

                                mapOf(
                                    "messages" to processedMessages,
                                    "messageCount" to messageCount
                                )
                            }
                        }
                    }
                }
                route("/reports") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                paginationParameters(this)
                            }
                            result {
                                val paginationParameters = getPaginationParameters(this@mongoOperation)

                                val (reports, reportsCount) = RocketchatDatabase().getReports(paginationParameters)

                                mapOf(
                                    "reports" to reports,
                                    "reportCount" to reportsCount
                                )
                            }
                        }
                    }
                }
                route("/channels/{channel}/stats") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "channel"; required = true }
                                queryParameter { name = "startDate" }
                                queryParameter { name = "endDate" }
                            }
                            result {
                                RocketchatStats().getChannelStats(parameter("channel")!!, dateParameter("startDate"), dateParameter("endDate"))
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
        }.start(wait = true)
    }

    private fun paginationParameters(parameters: Parameters) {
        parameters.queryParameter { name = "page"; datatype = Int::class; default = 1 }
        parameters.queryParameter { name = "limit"; datatype = Int::class; default = 100 }
        parameters.queryParameter { name = "sort"; required = true }
    }

    private fun getPaginationParameters(mongoOperation: MongoOperation): RocketchatDatabase.PaginationParameters {
        val page = mongoOperation.intParameter("page")!!
        val limit = mongoOperation.intParameter("limit")!!
        val sortAscending = when(mongoOperation.parameter("sort")) {
            "asc" -> true
            "desc" -> false
            else -> throw MongoOperationException("Invalid value for sort parameter",
                status = HttpStatusCode.BadRequest)
        }

        return RocketchatDatabase.PaginationParameters(page, limit, sortAscending)
    }
}
