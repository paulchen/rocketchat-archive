package at.rueckgr.rocketchat.archive

import at.rueckgr.rocketchat.ravusbot.RavusBotService
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

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
                            parameters {
                                urlParameter { name = "username"; required = true }
                            }
                            result {
                                val username = parameter("username")!!
                                val usernamesFromRavusBot = ravusBotService.getUsernames(username)

                                val usernames = usernamesFromRavusBot
                                    .ifEmpty { listOf(username) }
                                    .map { it.lowercase() }
                                val userDetails = RocketchatDatabase().getUserDetails(usernames)

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