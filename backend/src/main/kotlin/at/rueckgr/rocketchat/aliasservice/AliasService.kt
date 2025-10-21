package at.rueckgr.rocketchat.aliasservice

import at.rueckgr.rocketchat.archive.Logging
import at.rueckgr.rocketchat.archive.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder

class AliasService(private val aliasServiceEndpoint: String?, private val aliasServiceUsername: String?, private val aliasServicePassword: String?) : Logging {
    fun getUsernames(username: String): List<String> {
        if (aliasServiceEndpoint == null || aliasServiceUsername == null || aliasServicePassword == null) {
            logger().info("Alias service not (properly) configured, skipping call")
            return emptyList()
        }

        val encodedUsername = URLEncoder.encode(username, "utf-8")
        val url = aliasServiceEndpoint.replace("[USERNAME]", encodedUsername)
        val response = runBlocking {
            HttpClient(CIO) {
                install (Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(aliasServiceUsername, aliasServicePassword)
                        }
                    }
                }
            }.get {
                url(url)
            }.body<String>()
        }
        return response
            .lines()
            .filter { it.isNotBlank() }
    }
}
