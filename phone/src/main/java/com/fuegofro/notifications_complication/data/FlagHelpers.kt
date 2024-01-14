package com.fuegofro.notifications_complication.data

val notificationFlagNames: Array<String> =
    arrayOf(
        "FLAG_SHOW_LIGHTS",
        "FLAG_ONGOING_EVENT",
        "FLAG_INSISTENT",
        "FLAG_ONLY_ALERT_ONCE",
        "FLAG_AUTO_CANCEL",
        "FLAG_NO_CLEAR",
        "FLAG_FOREGROUND_SERVICE",
        "FLAG_HIGH_PRIORITY",
        "FLAG_LOCAL_ONLY",
        "FLAG_GROUP_SUMMARY",
        "FLAG_AUTOGROUP_SUMMARY",
        "FLAG_CAN_COLORIZE",
        "FLAG_BUBBLE",
        "FLAG_NO_DISMISS",
        "FLAG_FSI_REQUESTED_BUT_DENIED",
        "FLAG_USER_INITIATED_JOB",
    )

fun flagsToString(flags: Int, flagNames: Array<String>): String {
    var flagsRemaining = flags
    val result = mutableListOf<String>()
    var idx = 0
    while (flagsRemaining > 0) {
        if (flagsRemaining.mod(2) != 0) {
            result.add(flagNames.getOrElse(idx) { "<unknown ${1.shl(idx)}>" })
        }
        flagsRemaining = flagsRemaining.ushr(1)
        idx += 1
    }
    return if (result.isEmpty()) {
        "<none>"
    } else {
        result.joinToString("|")
    }
}
