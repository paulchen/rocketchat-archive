package at.rueckgr.rocketchat.archive

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Sorts.descending
import com.mongodb.kotlin.client.MongoDatabase
import io.ktor.http.*
import org.apache.commons.lang3.tuple.ImmutablePair
import org.bson.Document
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.ceil


class RocketchatDatabase : Logging {
    fun getUsers() = Mongo.getInstance().getDatabase()
        .getCollection<RocketchatUser>("users")
        .find()
        .map { mapUser(it) }
        .toList()
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username })

    fun getChannels() = Mongo.getInstance().getDatabase()
        .getCollection<RocketchatRoom>("rocketchat_room")
        .find(eq(RocketchatRoom::t.name, "c"))
        .map { mapChannel(it) }
        .toList()

    fun getPageForMessage(message: String, channel: String): Int {
        val timestamp = getMessage(message).ts

        val filterConditions = and(
            eq(RocketchatMessage::rid.name, channel),
            gt(RocketchatMessage::ts.name, timestamp),
            eq(RocketchatMessage::t.name, null),
            eq(RocketchatMessage::_hidden.name, null)
        )

        val count = Mongo.getInstance().getDatabase()
            .getCollection<RocketchatMessage>("rocketchat_message")
            .countDocuments(filterConditions)

        return ceil((count + 1) / 100.0).toInt()
    }

    private fun getMessagesByField(identifier: String, field: String): List<RocketchatMessage> {
        val dbMessages = Mongo.getInstance().getDatabase()
            .getCollection<RocketchatMessage>("rocketchat_message")
            .find(eq(field, identifier))
            .sort(descending(RocketchatMessage::editedAt.name))
            .toList()
        if (dbMessages.any { it.t != null }) {
            return emptyList()
        }
        return dbMessages
    }

    private fun getMessage(message: String) =
        getMessagesByField(message, RocketchatMessage::_id.name).firstOrNull()
            ?: throw MongoOperationException("Not found", status = HttpStatusCode.NotFound)

    private fun getMessagesByParent(parent: String) = getMessagesByField(parent, RocketchatMessage::parent.name)

    fun getMessages(channel: String, userIds: List<String>, text: String, date: LocalDate?, attachments: AttachmentType?, paginationParameters: PaginationParameters):
            ImmutablePair<Iterable<Message>, Long> {
        val filterConditions = mutableListOf(
            eq(RocketchatMessage::rid.name, channel),
            eq(RocketchatMessage::t.name, null),
            eq(RocketchatMessage::_hidden.name, null)
        )
        if (userIds.isNotEmpty() && !(userIds.size == 1 && userIds.first().isBlank())) {
            filterConditions.add(`in`("u._id", userIds))
        }
        if (text.isNotBlank()) {
            val pattern = try {
                Pattern.compile(text, Pattern.CASE_INSENSITIVE)
            }
            catch (e: PatternSyntaxException) {
                logger().info("Invalid regular expression in request: {}", text)
                throw MongoOperationException("Invalid regular expression", status = HttpStatusCode.BadRequest)
            }
            filterConditions.add(
                or(
                    regex("msg", pattern),
                    regex("attachments.description", pattern)
                )
            )
        }
        if (date != null) {
            val zonedDateTime: ZonedDateTime = ZonedDateTime.of(date.atStartOfDay(), ZoneId.systemDefault())
            filterConditions.add(gte(RocketchatMessage::ts.name, zonedDateTime))
            filterConditions.add(lt(RocketchatMessage::ts.name, zonedDateTime.plusDays(1)))
        }
        if (attachments != null) {
            when (attachments) {
                AttachmentType.ALL -> filterConditions.add(or(
                    exists("attachments.title"),
                    exists("attachments.message_link")
                ))
                AttachmentType.MESSAGE -> filterConditions.add(exists("attachments.message_link"))
                AttachmentType.IMAGE -> filterConditions.add(exists("attachments.image_url"))
                AttachmentType.AUDIO -> filterConditions.add(exists("attachments.audio_url"))
                AttachmentType.VIDEO -> filterConditions.add(exists("attachments.video_url"))
                AttachmentType.FILE -> filterConditions.add(and(
                    eq("attachments.type", "file"),
                    not(exists("attachments.message_link")),
                    not(exists("attachments.image_url")),
                    not(exists("attachments.audio_url")),
                    not(exists("attachments.video_url"))
                ))
            }
        }

        val messages = if (paginationParameters.sortAscending) {
            Mongo.getInstance().getDatabase()
                .getCollection<RocketchatMessage>("rocketchat_message")
                .find(and(filterConditions))
                .sort(ascending(RocketchatMessage::ts.name))
        }
        else {
            Mongo.getInstance().getDatabase()
                .getCollection<RocketchatMessage>("rocketchat_message")
                .find(and(filterConditions))
                .sort(descending(RocketchatMessage::ts.name))

        }
            .skip((paginationParameters.page - 1) * paginationParameters.limit)
            .limit(paginationParameters.limit)
            .map { mapMessage(it) }
            .toList()
        val messageCount = Mongo.getInstance().getDatabase()
            .getCollection<RocketchatMessage>("rocketchat_message")
            .countDocuments(and(filterConditions))

        return ImmutablePair(messages, messageCount)
    }

    fun getMessageHistory(message: String, channel: String): Iterable<Message> {
        val currentMessage = getMessage(message)
        if (currentMessage.rid != channel) {
            throw MongoOperationException("Not found", status = HttpStatusCode.NotFound)
        }

        val history = mutableListOf(mapMessage(currentMessage))
        val historyFromDatabase = getMessagesByParent(currentMessage._id).map { mapMessage(it) }
        if (historyFromDatabase.size > 0) {
            historyFromDatabase
                .zipWithNext()
                .forEach { history.add(Message(it.first.id, it.first.rid, it.first.message, it.first.timestamp, it.first.username, it.first.attachments, it.second.editedAt, it.second.editedBy)) }
            val last = historyFromDatabase.last()
            history.add(Message(last.id, last.rid, last.message, last.timestamp, last.username, last.attachments, last.timestamp, last.username))
        }
        return history
    }

    fun getReports(paginationParameters: PaginationParameters): ImmutablePair<Iterable<Report>, Long> {
        val database = Mongo.getInstance().getDatabase()
        val users = RocketchatDatabase().getUsers().associateBy(User::id)
        val channels = RocketchatDatabase().getChannels().map { it.id }
        val reports = if (paginationParameters.sortAscending) {
            database
                .getCollection<RocketchatReport>("rocketchat_reports")
                .find(`in`("message.rid", channels))
                .sort(ascending(RocketchatMessage::ts.name))
        }
        else {
            database
                .getCollection<RocketchatReport>("rocketchat_reports")
                .find(`in`("message.rid", channels))
                .sort(descending(RocketchatMessage::ts.name))
        }
            .skip((paginationParameters.page - 1) * paginationParameters.limit)
            .limit(paginationParameters.limit)
            .map { mapReport(it, users) }
            .toList()

        val reportsCount = database
            .getCollection<RocketchatReport>("rocketchat_reports")
            .countDocuments()

        return ImmutablePair(reports, reportsCount)
    }

    fun getUserByUsernames(usernames: List<String>): UserDetails {
        val database = Mongo.getInstance().getDatabase()
        val databaseUsers = database.getCollection<RocketchatUser>("users")
            .find()
            .map { User(it._id, it.name, it.username ?: it.name, it.__rooms ?: emptyList()) }
            .toList()
            .filter { usernames.contains(it.name.lowercase()) || usernames.contains(it.username.lowercase()) }
        if (databaseUsers.isEmpty()) {
            throw MongoOperationException("Unknown username", status = HttpStatusCode.NotFound)
        }
        val databaseUser = databaseUsers[0]

        val message = getMostRecentMessage(database, databaseUser.id)
        return UserDetails(databaseUser.id, databaseUser.username, message?.ts, databaseUser.rooms)
    }

    fun getUserById(userId: String): UserDetails {
        val database = Mongo.getInstance().getDatabase()
        val databaseUser = database.getCollection<RocketchatUser>("users")
            .find(eq(RocketchatUser::_id.name, userId))
            .firstOrNull() ?: throw MongoOperationException("Unknown user id", status = HttpStatusCode.NotFound)

        val message = getMostRecentMessage(database, databaseUser._id)
        return UserDetails(databaseUser._id, databaseUser.username ?: databaseUser.name, message?.ts, databaseUser.__rooms ?: emptyList())
    }

    private fun getMostRecentMessage(database: MongoDatabase, userId: String): RocketchatMessage? {
        val channelIds = getChannels().map { it.id }
        val message = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .find(
                and(
                    eq("u._id", userId),
                    eq(RocketchatMessage::t.name, null),
                    `in`(RocketchatMessage::rid.name, channelIds)
                )
            )
            .sort(descending(RocketchatMessage::ts.name))
            .limit(1)
            .firstOrNull()
        return message
    }

    fun getVersion(): String {
        val database = Mongo.getInstance().getDatabase()
        val result = database.runCommand(Document("buildInfo", 1))
        return result["version"].toString()
    }

    private fun mapUser(user: RocketchatUser) = User(user._id, user.name, user.username ?: user.name, user.__rooms ?: emptyList())

    private fun mapChannel(channel: RocketchatRoom) = Channel(channel.name!!, channel._id)

    private fun mapMessage(message: RocketchatMessage) =
        Message(
            message._id,
            message.rid,
            message.msg,
            message.ts,
            message.u.username,
            message.attachments?.map { mapAttachment(it) } ?: emptyList(),
            message.editedAt,
            message.editedBy?.username
        )

    private fun mapAttachment(attachment: RocketchatAttachment): Attachment {
        val type = if (attachment.message_link != null) {
            "message"
        }
        else if (attachment.image_url != null) {
            "image"
        }
        else if (attachment.audio_url != null) {
            "audio"
        }
        else if (attachment.video_url != null) {
            "video"
        }
        else {
            "file"
        }
        return Attachment(type, attachment.title, attachment.title_link, attachment.description, attachment.message_link)
    }
    private fun mapReport(report: RocketchatReport, users: Map<String, User>) =
        Report(report._id, mapMessage(report.message), report.description, report.ts, users[report.userId]!!)

    data class PaginationParameters(val page: Int, val limit: Int, val sortAscending: Boolean)

    enum class AttachmentType {
        ALL,
        MESSAGE,
        IMAGE,
        AUDIO,
        VIDEO,
        FILE
        ;
    }
}


