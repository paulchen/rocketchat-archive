package at.rueckgr.rocketchat.archive

import at.rueckgr.rocketchat.ravusbot.RavusBotService
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

data class RocketchatRoom(val _id: String, val t: String, val name: String?)

data class UserData(val _id: String, val username: String, val name: String?)

data class RocketchatMessage(val _id: String, val rid: String, val msg: String, val ts: ZonedDateTime, val u: UserData)

data class RocketchatUser(val _id: String, val name: String, val username: String)

data class Channel(val name: String, val id: String)

data class Message(val id: String, val message: String, val timestamp: ZonedDateTime, val username: String)

data class User(val id: String, val username: String)

data class UserDetails(val username: String, val timestamp: ZonedDateTime?)

fun main() {
    val ravusBotUsername = System.getenv("RAVUSBOT_USERNAME") ?: return
    val ravusBotPassword = System.getenv("RAVUSBOT_PASSWORD") ?: return

    val ravusBotService = RavusBotService(ravusBotUsername, ravusBotPassword)

    runBlocking {
        val serverForArchive = async {
            ServerForArchive(ravusBotService).start()
        }
        val serverForFrontend = async {
            ServerForFrontend().start()
        }

        serverForArchive.await()
        serverForFrontend.await()
    }

}
