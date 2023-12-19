package at.rueckgr.rocketchat.archive

import com.mongodb.client.model.Accumulators.first
import com.mongodb.client.model.Accumulators.sum
import com.mongodb.client.model.Aggregates.*
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Projections.computed
import com.mongodb.client.model.Projections.fields
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.mql.MqlValues
import com.mongodb.client.model.mql.MqlValues.current
import org.bson.conversions.Bson
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RocketchatStats {
    fun getChannelStats(channel: String, startDate: LocalDate?, endDate: LocalDate?): ChannelStats {
        val database = Mongo.getInstance().getDatabase()

        val users = database.getCollection<RocketchatUser>("users")
            .find()
            .toList()
            .associate { it._id to it.username }

        val firstMessageTimestamp = database.getCollection<RocketchatMessage>("rocketchat_message")
            .find(eq(RocketchatMessage::rid.name, channel))
            .sort(ascending(RocketchatMessage::ts.name))
            .limit(1)
            .firstOrNull()
            ?.ts ?: ZonedDateTime.now()

        val matchConditionsList = mutableListOf<Bson>()
        matchConditionsList.add(eq(RocketchatMessage::t.name, null))
        matchConditionsList.add(eq(RocketchatMessage::_hidden.name, null))
        if (channel != "all") {
            matchConditionsList.add(eq(RocketchatMessage::rid.name, channel))
        }
        if (startDate != null) {
            val zonedDateTime: ZonedDateTime = ZonedDateTime.of(startDate.atStartOfDay(), ZoneId.systemDefault())
            matchConditionsList.add(gte(RocketchatMessage::ts.name, zonedDateTime))
        }
        if (endDate != null) {
            val zonedDateTime: ZonedDateTime = ZonedDateTime.of(endDate.atStartOfDay(), ZoneId.systemDefault()).plusDays(1)
            matchConditionsList.add(lt(RocketchatMessage::ts.name, zonedDateTime))
        }
        val matchConditions = matchConditionsList.toTypedArray()

        val userMessageCount = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                listOf(
                    match(and(*matchConditions)),
                    project(
                        fields(
                            computed(StatsResult::key.name, "\$u._id"),
                            computed(StatsResult::value.name, current().getField(RocketchatMessage::rid.name).eq(MqlValues.ofNull()).cond(MqlValues.of(0), MqlValues.of(1)))
                        ),
                    ),
                    group(
                        "\$${StatsResult::key.name}",
                        first(StatsResult::key.name, "\$${StatsResult::key.name}"),
                        sum(StatsResult::value.name, "\$${StatsResult::value.name}")
                    ),
                    sort(
                        descending(StatsResult::value.name)
                    )
                )
            )
            .toList()
            .map { MessageCount(users[it.key]!!, it.value) }

        val now = MqlValues.of(ZonedDateTime.now().toInstant())
        val tz = MqlValues.of(ZoneId.systemDefault().id)

        val messagesPerMonth = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                listOf(
                    match(and(*matchConditions)),
                    project(
                        fields(
                            computed(StatsResult::key.name, current().getField(RocketchatMessage::ts.name).isDateOr(now).month(tz).asString()),
                            computed(StatsResult::additionalKey1.name, current().getField(RocketchatMessage::ts.name).isDateOr(now).year(tz).asString()),
                            computed(StatsResult::value.name, current().getField(RocketchatMessage::rid.name).eq(MqlValues.ofNull()).cond(MqlValues.of(0), MqlValues.of(1)))
                        )
                    ),
                    group(
                        fields(
                            computed(StatsResult::key.name, "\$${StatsResult::key.name}"),
                            computed(StatsResult::additionalKey1.name, "\$${StatsResult::additionalKey1.name}")
                        ),
                        first(StatsResult::key.name, "\$${StatsResult::key.name}"),
                        first(StatsResult::additionalKey1.name, "\$${StatsResult::additionalKey1.name}"),
                        sum(StatsResult::value.name, 1)
                    )
                )
            )
            .toList()
            .sortedBy { it.additionalKey1 + "-" + String.format("%02d", it.key.toInt()) }
            .map { MessageCount(it.additionalKey1 + "-" + String.format("%02d", it.key.toInt()), it.value) }

        val messagesPerYear = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                listOf(
                    match(and(*matchConditions)),
                    project(
                        fields(
                            computed(StatsResult::key.name, current().getField(RocketchatMessage::ts.name).isDateOr(now).year(tz).asString()),
                            computed(StatsResult::value.name, current().getField(RocketchatMessage::rid.name).eq(MqlValues.ofNull()).cond(MqlValues.of(0), MqlValues.of(1)))
                        )
                    ),
                    group(
                        "\$${StatsResult::key.name}",
                        first(StatsResult::key.name, "\$${StatsResult::key.name}"),
                        sum(StatsResult::value.name, "\$${StatsResult::value.name}")
                    ),
                    sort(
                        ascending(StatsResult::key.name)
                    )
                )
            )
            .toList()
            .map { MessageCount(it.key, it.value) }

        val topDays = database
            .getCollection<RocketchatMessage>("rocketchat_message")
            .aggregate<StatsResult>(
                listOf(
                    match(and(*matchConditions)),
                    project(
                        fields(
                            computed(StatsResult::key.name, current().getField(RocketchatMessage::ts.name).isDateOr(now).dayOfMonth(tz).asString()),
                            computed(StatsResult::additionalKey1.name, current().getField(RocketchatMessage::ts.name).isDateOr(now).month(tz).asString()),
                            computed(StatsResult::additionalKey2.name, current().getField(RocketchatMessage::ts.name).isDateOr(now).year(tz).asString()),
                            computed(StatsResult::value.name, current().getField(RocketchatMessage::rid.name).eq(MqlValues.ofNull()).cond(MqlValues.of(0), MqlValues.of(1)))
                        )
                    ),
                    group(
                        fields(
                            computed(StatsResult::key.name, "\$${StatsResult::key.name}"),
                            computed(StatsResult::additionalKey1.name, "\$${StatsResult::additionalKey1.name}"),
                            computed(StatsResult::additionalKey2.name, "\$${StatsResult::additionalKey2.name}")
                        ),
                        first(StatsResult::key.name, "\$${StatsResult::key.name}"),
                        first(StatsResult::additionalKey1.name, "\$${StatsResult::additionalKey1.name}"),
                        first(StatsResult::additionalKey2.name, "\$${StatsResult::additionalKey2.name}"),
                        sum(StatsResult::value.name, 1)
                    ),
                    sort(
                        descending(StatsResult::value.name)
                    ),
                    limit(10)
                )
            )
            .toList()
            .map {
                MessageCount(
                    it.additionalKey2 + "-" + String.format("%02d", it.additionalKey1!!.toInt()) + "-" + String.format("%02d", it.key.toInt()),
                    it.value
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
