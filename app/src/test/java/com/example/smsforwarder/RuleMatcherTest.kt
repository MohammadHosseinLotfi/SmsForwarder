package com.example.smsforwarder

import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.Rule
import com.example.smsforwarder.rules.RuleMatcher
import org.junit.Assert.*
import org.junit.Test

class RuleMatcherTest {

    @Test
    fun scenarioA_BluToUrl_matchesAndRendersJson() {
        val rule = Rule(
            id = 1,
            name = "بلو به URL",
            enabled = true,
            sender = "09999987641",
            containsText = "واریز پول",
            destinationType = DestinationType.URL,
            destinationAddress = "https://webhook.site/sample",
            jsonFields = mapOf("form" to "{{sender}}", "content" to "{{content}}")
        )

        val incomingSender = "+989999987641"
        val incomingContent = "بلو\nواریز پول\nمحمدحسین عزیز، 1,952,900 ریال به حساب شما نشست."

        assertTrue(RuleMatcher.matches(rule, incomingSender, incomingContent))

        val rendered = RuleMatcher.renderOutput(rule, incomingSender, incomingContent)
        assertTrue(rendered.contains("\"form\":\"+989999987641\""))
        assertTrue(rendered.contains("\"content\":\"بلو\\nواریز پول\\nمحمدحسین عزیز، 1,952,900 ریال به حساب شما نشست.\""))
    }

    @Test
    fun scenarioB_PasargadToSms_matchesAndRendersText() {
        val rule = Rule(
            id = 2,
            name = "پاسارگاد به SMS",
            enabled = true,
            sender = "B.Pasargad",
            containsText = "+",
            destinationType = DestinationType.SMS,
            destinationAddress = "09120000000",
            outgoingSlotIndex = 0,
            smsTemplate = "پیام پاسارگاد: {{content}}"
        )

        val incomingSender = "B.PASARGAD"
        val incomingContent = "+1,400,000 ریال واریز شد"

        assertTrue(RuleMatcher.matches(rule, incomingSender, incomingContent))

        val rendered = RuleMatcher.renderOutput(rule, incomingSender, incomingContent)
        assertEquals("پیام پاسارگاد: +1,400,000 ریال واریز شد", rendered)
    }

    @Test
    fun mismatchSender_returnsFalse() {
        val rule = Rule(
            id = 3,
            name = "تست",
            enabled = true,
            sender = "09121111111",
            destinationType = DestinationType.SMS,
            destinationAddress = "09122222222"
        )

        assertFalse(RuleMatcher.matches(rule, "09129999999", "هر متنی"))
    }
}