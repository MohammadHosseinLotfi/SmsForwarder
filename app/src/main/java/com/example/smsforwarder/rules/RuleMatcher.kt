package com.example.smsforwarder.rules

import com.google.gson.Gson

object RuleMatcher {

    private val gson = Gson()

    /**
     * بررسی برقراری شرط AND یک قانون روی فرستنده و متن دریافتی
     */
    fun matches(rule: Rule, rawSender: String, rawContent: String): Boolean {
        if (!rule.enabled) return false

        // ۱. بررسی شرط فرستنده
        val normalizedRuleSender = normalizeSender(rule.sender)
        val normalizedIncomingSender = normalizeSender(rawSender)

        if (normalizedRuleSender != normalizedIncomingSender) {
            return false
        }

        // ۲. بررسی شرط عبارت داخل متن (در صورت تعیین شدن)
        val filterText = rule.containsText?.trim()
        if (!filterText.isNullOrEmpty()) {
            val normalizedFilter = normalizeText(filterText)
            val normalizedBody = normalizeText(rawContent)
            if (!normalizedBody.contains(normalizedFilter)) {
                return false
            }
        }

        return true
    }

    /**
     * رندر قالب خروجی با جایگذاری متغیرهای {{sender}} و {{content}}
     */
    fun renderOutput(rule: Rule, rawSender: String, rawContent: String): String {
        return when (rule.destinationType) {
            DestinationType.SMS -> {
                rule.smsTemplate
                    .replace("{{sender}}", rawSender)
                    .replace("{{content}}", rawContent)
            }
            DestinationType.URL -> {
                val payloadMap = mutableMapOf<String, String>()
                for ((key, templateValue) in rule.jsonFields) {
                    val renderedVal = templateValue
                        .replace("{{sender}}", rawSender)
                        .replace("{{content}}", rawContent)
                    payloadMap[key] = renderedVal
                }
                gson.toJson(payloadMap)
            }
        }
    }

    /**
     * نرمال‌سازی شماره یا شناسه فرستنده
     */
    private fun normalizeSender(sender: String): String {
        val latinDigits = convertDigitsToLatin(sender.trim())

        // شناسه متنی مانند B.Pasargad
        if (latinDigits.any { it.isLetter() }) {
            return latinDigits.lowercase()
        }

        // حذف فاصله‌ها و خط تیره‌ها
        val cleanNumber = latinDigits.replace(" ", "").replace("-", "")

        // یکسان‌سازی پیش‌شماره‌های موبایل ایران (09..., +989..., 00989...)
        return when {
            cleanNumber.startsWith("+989") -> "0" + cleanNumber.substring(3)
            cleanNumber.startsWith("00989") -> "0" + cleanNumber.substring(4)
            cleanNumber.startsWith("989") -> "0" + cleanNumber.substring(2)
            else -> cleanNumber
        }
    }

    /**
     * نرمال‌سازی حروف و فاصله‌های متن برای تطبیق دقیق
     */
    private fun normalizeText(text: String): String {
        return text
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200C', ' ') // نیم‌فاصله به فاصله معمولی
            .replace(Regex("\\s+"), " ") // تجمیع فاصله‌ها و خطوط متوالی
            .trim()
    }

    private fun convertDigitsToLatin(str: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder()
        for (ch in str) {
            val pIdx = persian.indexOf(ch)
            val aIdx = arabic.indexOf(ch)
            when {
                pIdx != -1 -> sb.append(pIdx)
                aIdx != -1 -> sb.append(aIdx)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}