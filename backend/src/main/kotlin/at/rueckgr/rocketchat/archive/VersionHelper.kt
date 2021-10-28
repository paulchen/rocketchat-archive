package at.rueckgr.rocketchat.archive

class VersionHelper {
    companion object {
        val instance = VersionHelper()
    }

    fun getVersion(): String {
        return when (val resource = RocketchatMessage::class.java.getResource("/git-revision")) {
            null -> "unknown"
            else -> resource.readText().trim()
        }
    }
}
