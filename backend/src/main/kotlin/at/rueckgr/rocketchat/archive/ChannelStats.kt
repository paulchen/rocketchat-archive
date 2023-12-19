package at.rueckgr.rocketchat.archive

import java.time.ZonedDateTime

data class ChannelStats(val firstMessageDate: ZonedDateTime, val userMessageCount: List<MessageCount>, val timebasedMessageCounts: Map<String, TimebasedMessageCount>)

data class TimebasedMessageCount(val messageCounts: List<MessageCount>)

data class MessageCount(val key: String, val messages: Int)

data class StatsResult(val key: String, val additionalKey1: String?, val additionalKey2: String?, val value: Int)
