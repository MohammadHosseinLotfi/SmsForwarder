package com.example.smsforwarder.ui.util

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val monthName: String,
    val hourMinuteSecond: String
) {
    val formattedDate: String
        get() = "${day.toPersianDigits()} $monthName ${year.toPersianDigits()}"

    val formattedTimeShort: String
        get() {
            val parts = hourMinuteSecond.split(":")
            return if (parts.size >= 2) "${parts[0]}:${parts[1]}" else hourMinuteSecond
        }

    val fullFormatted: String
        get() = "$formattedDate - $formattedTimeShort"

    fun isSameDay(y: Int, m: Int, d: Int): Boolean {
        return this.year == y && this.month == m && this.day == d
    }
}

val PERSIAN_MONTH_NAMES = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

fun isJalaliLeapYear(year: Int): Boolean {
    val r = (((year - 474) % 2820) + 474 + 38) * 682
    return (r % 2816) < 682
}

fun jalaliDaysInMonth(year: Int, month: Int): Int {
    return when {
        month in 1..6 -> 31
        month in 7..11 -> 30
        isJalaliLeapYear(year) -> 30
        else -> 29
    }
}

fun getPersianMonthName(month: Int): String {
    return if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else ""
}

fun extractMoneyAmount(text: String): String? {
    if (text.isBlank()) return null
    val norm = text.toEnglishDigits()

    val regex1 = Regex("""(?:مبلغ|واریز|برداشت|مانده|انتقال)[:\s]*([0-9][0-9,٬.]*)\s*(ریال|تومان|تومن)?""", RegexOption.IGNORE_CASE)
    val match1 = regex1.find(norm)
    if (match1 != null) {
        val rawNum = match1.groupValues[1].replace(Regex("[^0-9]"), "")
        var unit = match1.groupValues.getOrNull(2)?.ifBlank { null } ?: "ریال"
        if (unit == "تومن") unit = "تومان"
        val valLong = rawNum.toLongOrNull()
        if (valLong != null && valLong > 0) {
            val formattedNum = String.format(Locale.US, "%,d", valLong)
            return "${formattedNum.toPersianDigits()} $unit"
        }
    }

    val regex2 = Regex("""([0-9]{1,3}(?:[,٬.][0-9]{3})+)\s*(ریال|تومان|تومن)?""")
    val match2 = regex2.find(norm)
    if (match2 != null) {
        val rawNum = match2.groupValues[1].replace(Regex("[^0-9]"), "")
        var unit = match2.groupValues.getOrNull(2)?.ifBlank { null } ?: "ریال"
        if (unit == "تومن") unit = "تومان"
        val valLong = rawNum.toLongOrNull()
        if (valLong != null && valLong > 0) {
            val formattedNum = String.format(Locale.US, "%,d", valLong)
            return "${formattedNum.toPersianDigits()} $unit"
        }
    }

    return null
}

fun timestampToJalali(timestamp: Long): JalaliDate {
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val gY = zdt.year
    val gM = zdt.monthValue
    val gD = zdt.dayOfMonth
    val hour = zdt.hour
    val minute = zdt.minute
    val second = zdt.second

    val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    val gy = gY - 1600
    var gm = gM - 1
    var gd = gD - 1

    var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
    for (i in 0 until gm) {
        gDayNo += gDaysInMonth[i + 1]
    }
    if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
        gDayNo++
    }
    gDayNo += gd

    var jDayNo = gDayNo - 79

    val jNp = jDayNo / 12053
    jDayNo %= 12053

    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461

    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }

    val jm: Int
    val jd: Int
    if (jDayNo < 186) {
        jm = 1 + jDayNo / 31
        jd = 1 + jDayNo % 31
    } else {
        jm = 7 + (jDayNo - 186) / 30
        jd = 1 + (jDayNo - 186) % 30
    }

    val monthName = PERSIAN_MONTH_NAMES[jm - 1]
    val timeStr = String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second).toPersianDigits()

    return JalaliDate(
        year = jy,
        month = jm,
        day = jd,
        monthName = monthName,
        hourMinuteSecond = timeStr
    )
}

fun String.toPersianDigits(): String {
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    val sb = StringBuilder(this.length)
    for (ch in this) {
        var replaced = false
        for (i in 0..9) {
            if (ch == englishDigits[i] || ch == arabicDigits[i]) {
                sb.append(persianDigits[i])
                replaced = true
                break
            }
        }
        if (!replaced) {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun Int.toPersianDigits(): String = this.toString().toPersianDigits()
fun Long.toPersianDigits(): String = this.toString().toPersianDigits()

fun String.toEnglishDigits(): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    val sb = StringBuilder(this.length)
    for (ch in this) {
        var replaced = false
        for (i in 0..9) {
            if (ch == persianDigits[i] || ch == arabicDigits[i]) {
                sb.append(englishDigits[i])
                replaced = true
                break
            }
        }
        if (!replaced) {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun String.fixPersianText(): String {
    return this.replace('ي', 'ی').replace('ك', 'ک')
}

data class SenderBadgeInfo(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
)

fun detectSenderBadge(sender: String): SenderBadgeInfo? {
    if (sender.isBlank()) return null
    val normSender = sender.toEnglishDigits().lowercase().trim()
    val fixedSender = sender.fixPersianText().lowercase().trim()

    return when {
        normSender.contains("blu") || fixedSender.contains("بلو") ->
            SenderBadgeInfo("بلوبانک", Color(0xFF1E88E5), Color.White)
        normSender.contains("pasargad") || fixedSender.contains("پاسارگاد") ->
            SenderBadgeInfo("بانک پاسارگاد", Color(0xFFD97706), Color.White)
        normSender.contains("mellat") || fixedSender.contains("ملت") || normSender.contains("98200000") ->
            SenderBadgeInfo("بانک ملت", Color(0xFFDC2626), Color.White)
        normSender.contains("melli") || fixedSender.contains("ملی") || normSender.contains("bam") ->
            SenderBadgeInfo("بانک ملی", Color(0xFF0288D1), Color.White)
        normSender.contains("saman") || fixedSender.contains("سامان") ->
            SenderBadgeInfo("بانک سامان", Color(0xFF00ACC1), Color.White)
        normSender.contains("tejarat") || fixedSender.contains("تجارت") ->
            SenderBadgeInfo("بانک تجارت", Color(0xFF00897B), Color.White)
        normSender.contains("parsian") || fixedSender.contains("پارسیان") ->
            SenderBadgeInfo("بانک پارسیان", Color(0xFFB45309), Color.White)
        normSender.contains("keshavarzi") || fixedSender.contains("کشاورزی") ->
            SenderBadgeInfo("بانک کشاورزی", Color(0xFF15803D), Color.White)
        normSender.contains("maskan") || fixedSender.contains("مسکن") ->
            SenderBadgeInfo("بانک مسکن", Color(0xFFEA580C), Color.White)
        normSender.startsWith("09") || normSender.startsWith("+989") || normSender.startsWith("989") ->
            SenderBadgeInfo("شماره موبایل", Color(0xFF4F46E5), Color.White)
        normSender.all { it.isDigit() || it == '+' } ->
            SenderBadgeInfo("سرشماره خدماتی", Color(0xFF475569), Color.White)
        else -> null
    }
}

fun detectMobileOperator(number: String): String? {
    val norm = number.toEnglishDigits().replace(Regex("[^0-9]"), "")
    val target = when {
        norm.startsWith("989") -> "09" + norm.substring(3)
        norm.startsWith("9") -> "0" + norm
        else -> norm
    }
    if (target.length < 4) return null
    val prefix4 = target.take(4)
    return when {
        prefix4 in listOf("0910", "0911", "0912", "0913", "0914", "0915", "0916", "0917", "0918", "0919", "0990", "0991", "0992", "0993", "0994") -> "همراه اول"
        prefix4 in listOf("0930", "0933", "0935", "0936", "0937", "0938", "0939", "0901", "0902", "0903", "0904", "0905", "0941") -> "ایرانسل"
        prefix4 in listOf("0920", "0921", "0922", "0923") -> "رایتل"
        else -> null
    }
}
