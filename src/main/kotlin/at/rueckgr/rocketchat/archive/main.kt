import org.litote.kmongo.*
import java.time.LocalDateTime

data class RocketchatRoom(val _id: String, val t: String, val name: String?)

data class UserData(val _id: String, val username: String, val name: String)

data class RocketchatMessage(val _id: String, val rid: String, val msg: String, val ts: LocalDateTime, val u: UserData)

data class RocketchatUser(val _id: String, val name: String, val username: String)

fun main() {
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
}
