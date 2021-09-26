package at.rueckgr.rocketchat.ravusbot

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder

class RavusBotService(private val ravusBotUsername: String, private val ravusBotPassword: String) {
    fun getUsernames(username: String): List<String> {
        val encodedUsername = URLEncoder.encode(username, "utf-8")
        val response = runBlocking {
            // TODO http basic auth
            HttpClient(CIO) {
                install (Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(ravusBotUsername, ravusBotPassword)
                        }
                    }
                }
            }.request<String> {
                url("https://ondrahosek.com/ravusbot/aliases?nick=$encodedUsername")
                method = HttpMethod.Get
            }
        }
        return response
            .lines()
            .filter { it.isNotBlank() }
    }
}
