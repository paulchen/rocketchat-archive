package at.rueckgr.rocketchat.archive

data class ChannelStats(val userMessageCount: List<MessageCount>, val timebasedMessageCounts: Map<String, TimebasedMessageCount>)

data class TimebasedMessageCount(val messageCounts: List<MessageCount>)

data class MessageCount(val key: String, val messages: Int)

data class StatsResult(val key: String, val additionalKey1: String?, val additionalKey2: String?, val value: String)
