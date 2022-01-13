package at.rueckgr.rocketchat.archive

data class ArchiveConfiguration(val mongoUrl: String = "mongodb://mongo:27017", val database: String = "rocketchat")

class ConfigurationProvider {
    val archiveConfiguration: ArchiveConfiguration

    companion object {
        val instance = ConfigurationProvider()

        fun getConfiguration(): ArchiveConfiguration {
            return this.instance.archiveConfiguration
        }
    }

    init {
        val databaseName = System.getenv("DATABASE")

        this.archiveConfiguration = ArchiveConfiguration(database = databaseName)
    }
}
