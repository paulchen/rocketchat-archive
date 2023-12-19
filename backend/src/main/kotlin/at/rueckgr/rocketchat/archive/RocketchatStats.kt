package at.rueckgr.rocketchat.archive

import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.litote.kmongo.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RocketchatStats {
    fun getChannelStats(channel: String, startDate: LocalDate?, endDate: LocalDate?): ChannelStats {
        val database = Mongo.getInstance().getDatabase()

        val users = database.getCollection<RocketchatUser>("users")
            .find()
            .associate { it._id to it.username }

        val firstMessageTimestamp = database.getCollection<RocketchatMessage>("rocketchat_message")
            .find(RocketchatMessage::rid eq channel)
            .ascendingSort(RocketchatMessage::ts)
            .limit(1)
            .singleOrNull()
            ?.ts ?: ZonedDateTime.now()

        val matchConditionsList = mutableListOf<Bson>()
        matchConditionsList.add(RocketchatMessage::t eq null)
        matchConditionsList.add(RocketchatMessage::_hidden eq null)
        if (channel != "all") {
            matchConditionsList.add(RocketchatMessage::rid eq channel)
        }
        if (startDate != null) {
            val zonedDateTime: ZonedDateTime = ZonedDateTime.of(startDate.atStartOfDay(), ZoneId.systemDefault())
            matchConditionsList.add(RocketchatMessage::ts gte zonedDateTime)
        }
        if (endDate != null) {
            val zonedDateTime: ZonedDateTime = ZonedDateTime.of(endDate.atStartOfDay(), ZoneId.systemDefault()).plusDays(1)
            matchConditionsList.add(RocketchatMessage::ts lt zonedDateTime)
        }
        val matchConditions = matchConditionsList.toTypedArray()

        val userMessageCount = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                match(*matchConditions),
                project(
                    StatsResult::key from RocketchatMessage::u / UserData::_id,
                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                ),
                group(
                    StatsResult::key,
                    StatsResult::key first StatsResult::key,
                    StatsResult::value sum StatsResult::value
                ),
                sort(
                    descending(StatsResult::value)
                )
            )
            .toList()
            .map { MessageCount(users[it.key]!!, it.value.toInt()) }

        val messagesPerMonth = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                match(*matchConditions),
                project(
                    StatsResult::key from month(RocketchatMessage::ts),
                    StatsResult::additionalKey1 from year(RocketchatMessage::ts),
                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                ),
                group(
                    fields(StatsResult::key from StatsResult::key, StatsResult::additionalKey1 from StatsResult::additionalKey1),
                    StatsResult::key first StatsResult::key,
                    StatsResult::additionalKey1 first StatsResult::additionalKey1,
                    StatsResult::value sum 1
                )
            )
            .toList()
            .sortedBy { it.additionalKey1 + "-" + String.format("%02d", it.key.toInt()) }
            .map { MessageCount(it.additionalKey1 + "-" + String.format("%02d", it.key.toInt()), it.value.toInt()) }

        val messagesPerYear = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                match(*matchConditions),
                project(
                    StatsResult::key from year(RocketchatMessage::ts),
                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                ),
                group(
                    StatsResult::key,
                    StatsResult::key first StatsResult::key,
                    StatsResult::value sum StatsResult::value
                ),
                sort(
                    ascending(StatsResult::key)
                )
            )
            .toList()
            .map { MessageCount(it.key, it.value.toInt()) }

        val topDays = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                match(*matchConditions),
                project(
                    StatsResult::key from dayOfMonth(RocketchatMessage::ts),
                    StatsResult::additionalKey1 from month(RocketchatMessage::ts),
                    StatsResult::additionalKey2 from year(RocketchatMessage::ts),
                    StatsResult::value from cond(RocketchatMessage::rid, 1, 0)
                ),
                group(
                    fields(
                        StatsResult::key from StatsResult::key,
                        StatsResult::additionalKey1 from StatsResult::additionalKey1,
                        StatsResult::additionalKey2 from StatsResult::additionalKey2
                    ),
                    StatsResult::key first StatsResult::key,
                    StatsResult::additionalKey1 first StatsResult::additionalKey1,
                    StatsResult::additionalKey2 first StatsResult::additionalKey2,
                    StatsResult::value sum 1
                ),
                sort(
                    descending(StatsResult::value)
                ),
                limit(10)
            )
            .toList()
            .map {
                MessageCount(
                    it.additionalKey2 + "-" + String.format("%02d", it.additionalKey1!!.toInt()) + "-" + String.format("%02d", it.key.toInt()),
                    it.value.toInt()
                )
            }

        return ChannelStats(
            firstMessageTimestamp,
            userMessageCount,
            mapOf(
                "messagesPerMonth" to TimebasedMessageCount(messagesPerMonth),
                "messagesPerYear" to TimebasedMessageCount(messagesPerYear),
                "topDays" to TimebasedMessageCount(topDays)
            )
        )

    }
}
