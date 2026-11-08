package com.jarvis.assistant

import org.junit.Assert.*
import org.junit.Test

class ParsingTest {
    @Test fun alarmPm() = assertEquals(18 to 30, Parsing.alarmTime("set an alarm for 6:30 pm"))
    @Test fun alarmAmMidnight() = assertEquals(0 to 5, Parsing.alarmTime("alarm 12:05 am"))
    @Test fun alarmInvalid() = assertNull(Parsing.alarmTime("alarm 25"))
    @Test fun openTarget() = assertEquals("whatsapp", Parsing.openTarget("Open WhatsApp"))
    @Test fun callTarget() = assertEquals("home", Parsing.callTarget("call home"))
    @Test fun navTarget() = assertEquals("college", Parsing.navTarget("navigate to college"))
    @Test fun malayalam() { assertTrue(Parsing.isMalayalam("നാളെ")); assertFalse(Parsing.isMalayalam("hello")) }
    @Test fun stop() { assertTrue(Parsing.isStop("Emergency stop")); assertFalse(Parsing.isStop("stop by the shop")) }
}
