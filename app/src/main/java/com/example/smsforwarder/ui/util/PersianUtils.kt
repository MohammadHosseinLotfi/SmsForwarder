package com.example.smsforwarder.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.LinkedHashMap
import java.util.Locale

@Immutable
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

private val MONEY_REGEX_1 = Regex("""(?:مبلغ|واریز|برداشت|مانده|انتقال)[:\s]*([0-9][0-9,٬.]*)\s*(ریال|تومان|تومن)?""", RegexOption.IGNORE_CASE)
private val MONEY_REGEX_2 = Regex("""([0-9]{1,3}(?:[,٬.][0-9]{3})+)\s*(ریال|تومان|تومن)?""")
private val NON_DIGIT_REGEX = Regex("[^0-9]")

private val MCI_PREFIXES = setOf("0910", "0911", "0912", "0913", "0914", "0915", "0916", "0917", "0918", "0919", "0990", "0991", "0992", "0993", "0994")
private val MTN_PREFIXES = setOf("0930", "0933", "0935", "0936", "0937", "0938", "0939", "0901", "0902", "0903", "0904", "0905", "0941")
private val RIGHTEL_PREFIXES = setOf("0920", "0921", "0922", "0923")

private class SimpleLruCache<K, V>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }
}

private val jalaliCache = SimpleLruCache<Long, JalaliDate>(512)
private val moneyCache = SimpleLruCache<String, String>(256)
private const val NO_MONEY_SENTINEL = "<NONE>"

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
    val cached = moneyCache.get(text)
    if (cached != null) {
        return if (cached == NO_MONEY_SENTINEL) null else cached
    }

    val result = computeExtractMoneyAmount(text)
    moneyCache.put(text, result ?: NO_MONEY_SENTINEL)
    return result
}

private fun computeExtractMoneyAmount(text: String): String? {
    val norm = text.toEnglishDigits()

    val match1 = MONEY_REGEX_1.find(norm)
    if (match1 != null) {
        val rawNum = match1.groupValues[1].replace(NON_DIGIT_REGEX, "")
        var unit = match1.groupValues.getOrNull(2)?.ifBlank { null } ?: "ریال"
        if (unit == "تومن") unit = "تومان"
        val valLong = rawNum.toLongOrNull()
        if (valLong != null && valLong > 0) {
            val formattedNum = String.format(Locale.US, "%,d", valLong)
            return "${formattedNum.toPersianDigits()} $unit"
        }
    }

    val match2 = MONEY_REGEX_2.find(norm)
    if (match2 != null) {
        val rawNum = match2.groupValues[1].replace(NON_DIGIT_REGEX, "")
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
    val cached = jalaliCache.get(timestamp)
    if (cached != null) return cached

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

    val result = JalaliDate(
        year = jy,
        month = jm,
        day = jd,
        monthName = monthName,
        hourMinuteSecond = timeStr
    )
    jalaliCache.put(timestamp, result)
    return result
}

fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
    val jy1 = jy - 979
    val jm1 = jm - 1
    val jd1 = jd - 1

    var jDayNo = 365 * jy1 + (jy1 / 33) * 8 + ((jy1 % 33) + 3) / 4
    for (i in 0 until jm1) {
        jDayNo += if (i < 6) 31 else 30
    }
    jDayNo += jd1

    var gDayNo = jDayNo + 79

    var gy = 1600 + 400 * (gDayNo / 146097)
    gDayNo %= 146097

    var leap = true
    if (gDayNo >= 36525) {
        gDayNo--
        gy += 100 * (gDayNo / 36524)
        gDayNo %= 36524

        if (gDayNo >= 365) {
            gDayNo++
        } else {
            leap = false
        }
    }

    gy += 4 * (gDayNo / 1461)
    gDayNo %= 1461

    if (gDayNo >= 366) {
        leap = false
        gDayNo--
        gy += gDayNo / 365
        gDayNo %= 365
    }

    val gDaysInMonth = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gm = 0
    while (gDayNo >= gDaysInMonth[gm + 1]) {
        gDayNo -= gDaysInMonth[gm + 1]
        gm++
    }
    val gd = gDayNo + 1
    return intArrayOf(gy, gm + 1, gd)
}

fun getJalaliFirstDayOfWeek(year: Int, month: Int): Int {
    val gDate = jalaliToGregorian(year, month, 1)
    val localDate = LocalDate.of(gDate[0], gDate[1], gDate[2])
    return when (localDate.dayOfWeek) {
        DayOfWeek.SATURDAY -> 0
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
        null -> 0
    }
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

@Immutable
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
    val norm = number.toEnglishDigits().replace(NON_DIGIT_REGEX, "")
    val target = when {
        norm.startsWith("989") -> "09" + norm.substring(3)
        norm.startsWith("9") -> "0" + norm
        else -> norm
    }
    if (target.length < 4) return null
    val prefix4 = target.take(4)
    return when (prefix4) {
        in MCI_PREFIXES -> "همراه اول"
        in MTN_PREFIXES -> "ایرانسل"
        in RIGHTEL_PREFIXES -> "رایتل"
        else -> null
    }
}
