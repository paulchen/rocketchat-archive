package at.rueckgr.rocketchat.archive

import java.io.InputStream
import java.util.*

class VersionHelper {
    companion object {
        val instance = VersionHelper()
    }

    fun getVersion(): VersionInfo {
        return when (val resource = RocketchatMessage::class.java.getResourceAsStream("/git-revision")) {
            null -> VersionInfo("unknown", "unknown")
            else -> readVersionInfo(resource)
        }
    }

    private fun readVersionInfo(stream: InputStream): VersionInfo {
        val properties = Properties()
        properties.load(stream)

        return VersionInfo(properties.getProperty("revision"), properties.getProperty("commitMessage"))
    }
}
