package at.rueckgr.rocketchat.archive

import at.rueckgr.rocketchat.aliasservice.AliasService
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

@Suppress("ExtractKtorModule")
class RestEndpointForBot(private val aliasService: AliasService) {
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
                            parameters {
                                urlParameter { name = "username"; required = true }
                            }
                            result {
                                val username = parameter("username")!!
                                val usernamesFromAliasService = aliasService.getUsernames(username)

                                val usernames = usernamesFromAliasService
                                    .ifEmpty { listOf(username) }
                                    .map { it.lowercase() }
                                val userDetails = RocketchatDatabase().getUserByUsernames(usernames)

                                mapOf("user" to userDetails)
                            }
                        }
                    }
                }
                route("/user/id/{id}") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "id"; required = true }
                            }
                            result {
                                mapOf("user" to RocketchatDatabase().getUserById(parameter("id")!!))
                            }
                        }
                    }
                }
                route("/version") {
                    get {
                        mongoOperation(this) {
                            result {
                                mapOf(
                                    "version" to VersionHelper.instance.getVersion(),
                                    "mongoDbVersion" to RocketchatDatabase().getVersion()
                                )
                            }
                        }
                    }
                }
                route("/channel/{channelId}") {
                    get {
                        mongoOperation(this) {
                            parameters {
                                urlParameter { name = "channelId"; required = true }
                            }
                            result {
                                val channelId = parameter("channelId")!!
                                val paginationParameters = RocketchatDatabase.PaginationParameters(1, 1, false)
                                val channelName = RocketchatDatabase().getChannels().find { it.id == channelId }?.name
                                val (messages, _) = RocketchatDatabase().getMessages(channelId, emptyList(), "", null, null, paginationParameters)

                                val lastActivity = when (messages.any()) {
                                    true -> messages.first().timestamp
                                    false -> null
                                }
                                mapOf(
                                    "id" to channelId,
                                    "name" to channelName,
                                    "lastActivity" to lastActivity
                                )
                            }
                        }
                    }
                }
            }
        }.start()
    }
}
