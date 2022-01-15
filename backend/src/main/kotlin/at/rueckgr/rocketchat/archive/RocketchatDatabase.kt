package at.rueckgr.rocketchat.archive

import com.mongodb.client.model.Filters
import io.ktor.http.*
import org.apache.commons.lang3.tuple.ImmutablePair
import org.litote.kmongo.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.ceil

class RocketchatDatabase : Logging {
    fun getUsers() = Mongo.getInstance().getDatabase()
        .getCollection<RocketchatUser>("users")
        .find()
        .map { mapUser(it) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username })

    fun getChannels() = Mongo.getInstance().getDatabase()
        .getCollection<RocketchatRoom>("rocketchat_room")
        .find()
        .filter { it.t == "c" }
        .map { mapChannel(it) }

    fun getPageForMessage(message: String, channel: String): Int {
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

        return ceil((count + 1) / 100.0).toInt()
    }

    fun getMessages(channel: String, userIds: List<String>, text: String, paginationParameters: PaginationParameters):
            ImmutablePair<Iterable<Message>, Int> {
        val filterConditions = mutableListOf(RocketchatMessage::rid eq channel, RocketchatMessage::t eq null)
        if (userIds.isNotEmpty() && !(userIds.size == 1 && userIds.first().isBlank())) {
            filterConditions.add(RocketchatMessage::u / UserData::_id `in` userIds)
        }
        if (text.isNotBlank()) {
            try {
                filterConditions.add(Filters.regex("msg", Pattern.compile(text, Pattern.CASE_INSENSITIVE)))
            }
            catch (e: PatternSyntaxException) {
                logger().info("Invalid regular expression in request: {}", text)
                throw MongoOperationException("Invalid regular expression", status = HttpStatusCode.BadRequest)
            }
        }

        val messages = if (paginationParameters.sortAscending) {
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
            .skip((paginationParameters.page - 1) * paginationParameters.limit)
            .limit(paginationParameters.limit)
            .map { mapMessage(it) }
        val messageCount = Mongo.getInstance().getDatabase()
            .getCollection<RocketchatMessage>("rocketchat_message")
            .find(and(filterConditions))
            .count()

        return ImmutablePair(messages, messageCount)
    }

    fun getReports(paginationParameters: PaginationParameters): ImmutablePair<Iterable<Report>, Int> {
        val database = Mongo.getInstance().getDatabase()
        val users = RocketchatDatabase().getUsers().associateBy(User::id)
        val channels = RocketchatDatabase().getChannels().map { it.id }
        val reports = if (paginationParameters.sortAscending) {
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
            .skip((paginationParameters.page - 1) * paginationParameters.limit)
            .limit(paginationParameters.limit)
            .filter { channels.contains(it.message.rid) }
            .map { mapReport(it, users) }

        val reportsCount = database
            .getCollection<RocketchatReport>("rocketchat_reports")
            .find()
            .count()

        return ImmutablePair(reports, reportsCount)
    }

    fun getUserDetails(usernames: List<String>): UserDetails {
        val database = Mongo.getInstance().getDatabase()
        val databaseUsers = database.getCollection<RocketchatUser>("users")
            .find()
            .map { User(it._id, it.name, it.username) }
            .filter { usernames.contains(it.name.lowercase()) || usernames.contains(it.username.lowercase()) }
        if (databaseUsers.isEmpty()) {
            throw MongoOperationException("Unknown username", status = HttpStatusCode.NotFound)
        }
        val databaseUser = databaseUsers[0]

        val message = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .find(
                and(
                    RocketchatMessage::u / UserData::_id eq databaseUser.id,
                    RocketchatMessage::t eq null
                )
            )
            .descendingSort(RocketchatMessage::ts)
            .limit(1)
            .singleOrNull()

        return UserDetails(databaseUser.username, message?.ts)
    }

    private fun mapUser(user: RocketchatUser) = User(user._id, user.name, user.username)

    private fun mapChannel(channel: RocketchatRoom) = Channel(channel.name!!, channel._id)

    private fun mapMessage(message: RocketchatMessage) =
        Message(message._id, message.msg, message.ts, message.u.username)

    private fun mapReport(report: RocketchatReport, users: Map<String, User>) =
        Report(report._id, mapMessage(report.message), report.description, report.ts, users[report.userId]!!)

    data class PaginationParameters(val page: Int, val limit: Int, val sortAscending: Boolean)
}


