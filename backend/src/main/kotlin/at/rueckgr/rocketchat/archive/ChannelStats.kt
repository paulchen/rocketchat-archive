package at.rueckgr.rocketchat.archive

data class ChannelStats(val userMessageCount: Map<String, Int>, val timebasedMessageCounts: Map<String, TimebasedMessageCount>)

data class TimebasedMessageCount(val messageCounts: Map<String, Int>)

data class StatsResult(val key: String, val additionalKey1: String?, val additionalKey2: String?, val value: String)
