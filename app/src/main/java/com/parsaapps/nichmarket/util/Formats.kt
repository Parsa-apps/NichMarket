package com.parsaapps.nichmarket.util

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

/** تاریخ شمسی مطابق منطقه زمانی دستگاه (Android ICU، از API 24). */
fun formatShamsi(epochMs: Long): String {
    val cal = android.icu.util.PersianCalendar()
    cal.timeInMillis = epochMs
    val year = cal.get(android.icu.util.Calendar.YEAR)
    val month = cal.get(android.icu.util.Calendar.MONTH) + 1
    val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
    return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}".toPersianDigits()
}
