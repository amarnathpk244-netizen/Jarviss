package com.jarvis.assistant

/** Pure text helpers (unit-testable, no Android dependencies). */
object Parsing {
    fun openTarget(t: String) = Regex("""^(?:open|launch|start)\s+(.+)$""").find(t.trim().lowercase())?.groupValues?.get(1)?.trim()
    fun callTarget(t: String) = Regex("""^call\s+(.+)$""").find(t.trim().lowercase())?.groupValues?.get(1)?.trim()
    fun navTarget(t: String) =
        Regex("""^(?:navigate to|take me to|directions to)\s+(.+)$""").find(t.trim().lowercase())?.groupValues?.get(1)?.trim()

    /** "set an alarm for 6:30 pm" -> (18, 30) */
    fun alarmTime(t: String): Pair<Int, Int>? {
        val m = Regex("""alarm.*?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(t.lowercase()) ?: return null
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].ifEmpty { "0" }.toInt()
        val ap = m.groupValues[3]
        if (ap == "pm" && h < 12) h += 12
        if (ap == "am" && h == 12) h = 0
        return if (h > 23 || min > 59) null else h to min
    }

    fun isMalayalam(s: String) = s.any { it in '\u0D00'..'\u0D7F' }

    fun isStop(t: String): Boolean {
        val s = t.trim().lowercase()
        return s in setOf("stop", "cancel", "emergency stop", "never mind", "നിർത്തൂ", "നിർത്തു")
    }
}
