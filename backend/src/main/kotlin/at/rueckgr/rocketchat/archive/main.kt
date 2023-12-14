package at.rueckgr.rocketchat.archive

import at.rueckgr.rocketchat.ravusbot.RavusBotService
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import java.time.ZonedDateTime

data class RocketchatRoom(val _id: String, val t: String, val name: String?)

data class UserData(val _id: String, val username: String, val name: String?)

data class RocketchatMessage(val _id: String, val rid: String, val msg: String, val ts: ZonedDateTime, val u: UserData, val t: String?, val attachments: List<RocketchatAttachment>?)

data class RocketchatAttachment(val type: String?, val title: String?, val title_link: String?, val description: String?)

data class RocketchatUser(val _id: String, val name: String, val username: String?, val __rooms: List<String>?)

data class RocketchatReport(val _id: String, val message: RocketchatMessage, val description: String, val ts: ZonedDateTime, val userId: String)

data class Channel(val name: String, val id: String)

data class Message(val id: String, val rid: String, val message: String, val timestamp: ZonedDateTime, val username: String, val attachments: List<Attachment>)

data class Attachment(val type: String, val title: String?, val titleLink: String?, val description: String?)

data class User(val id: String, val name: String, val username: String, val rooms: List<String>)

data class UserDetails(val id: String, val username: String, val timestamp: ZonedDateTime?, val rooms: List<String>)

data class Report(val id: String, val message: Message, val description: String, val timestamp: ZonedDateTime, val reporter: User)

fun main() {
    val ravusBotUsername = System.getenv("RAVUSBOT_USERNAME") ?: return
    val ravusBotPassword = System.getenv("RAVUSBOT_PASSWORD") ?: return

    val ravusBotService = RavusBotService(ravusBotUsername, ravusBotPassword)

    if (StringUtils.isBlank(ConfigurationProvider.getConfiguration().database)) {
        return
    }

    RestEndpointForBot(ravusBotService).start()
    RestEndpointForFrontend().start()
}
