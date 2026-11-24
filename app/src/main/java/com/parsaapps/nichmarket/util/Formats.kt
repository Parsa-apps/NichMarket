package com.parsaapps.nichmarket.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تبدیل ارقام لاتین به فارسی */
fun String.toPersianDigits(): String {
    val fa = "۰۱۲۳۴۵۶۷۸۹"
    return buildString {
        for (c in this@toPersianDigits) {
            if (c in '0'..'9') append(fa[c - '0']) else append(c)
        }
    }
}

fun Int.toPersian(): String = toString().toPersianDigits()

/** قیمت با جداکننده هزارگان و پسوند تومان */
fun formatToman(toman: Int): String =
    "%,d".format(toman).toPersianDigits() + " تومان"

/** زمان شمارش معکوس به شکل mm:ss */
fun formatClock(totalSeconds: Int): String {
    val m = (totalSeconds / 60).toPersian()
    val s = (totalSeconds % 60).toString().padStart(2, '0').toPersianDigits()
    return "$m:$s"
}

/** تاریخ شمسی از timestamp */
fun formatShamsi(epochMs: Long): String = try {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR"))
    sdf.format(Date(epochMs))
} catch (t: Throwable) {
    ""
}
