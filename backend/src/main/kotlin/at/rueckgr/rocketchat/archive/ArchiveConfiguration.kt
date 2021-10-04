package at.rueckgr.rocketchat.archive

data class ArchiveConfiguration(val mongoUrl: String = "mongodb://mongo:27017", val database: String = "rocketchat")
