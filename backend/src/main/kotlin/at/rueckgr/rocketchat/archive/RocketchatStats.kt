package at.rueckgr.rocketchat.archive

import org.litote.kmongo.*

class RocketchatStats {
    fun getChannelStats(channel: String) {
        val database = Mongo.getInstance().getDatabase()

        val users = database.getCollection<RocketchatUser>("users")
            .find()
            .associate { it._id to it.username }

        val userMessageCount = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                match(RocketchatMessage::rid eq channel, RocketchatMessage::t eq null),
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
                match(RocketchatMessage::rid eq channel, RocketchatMessage::t eq null),
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
                match(RocketchatMessage::rid eq channel, RocketchatMessage::t eq null),
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
                match(RocketchatMessage::rid eq channel, RocketchatMessage::t eq null),
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

        ChannelStats(
            userMessageCount,
            mapOf(
                "messagesPerMonth" to TimebasedMessageCount(messagesPerMonth),
                "messagesPerYear" to TimebasedMessageCount(messagesPerYear),
                "topDays" to TimebasedMessageCount(topDays)
            )
        )

    }
}
