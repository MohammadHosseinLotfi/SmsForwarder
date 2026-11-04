package com.example.smsforwarder

import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.Rule
import com.example.smsforwarder.rules.RuleDestination
import com.example.smsforwarder.rules.RuleMatcher
import org.junit.Assert.*
import org.junit.Test

class RuleMatcherTest {

    @Test
    fun scenarioA_BluToUrl_matchesAndRendersJson() {
        val dest = RuleDestination(
            destinationType = DestinationType.URL,
            destinationAddress = "https://webhook.site/sample",
            jsonFields = mapOf("form" to "{{sender}}", "content" to "{{content}}")
        )
        val rule = Rule(
            id = 1,
            name = "بلو به URL",
            enabled = true,
            sender = "09999987641",
            containsText = "واریز پول",
            destinations = listOf(dest)
        )

        val incomingSender = "+989999987641"
        val incomingContent = "بلو\nواریز پول\nمحمدحسین عزیز، 1,952,900 ریال به حساب شما نشست."

        assertTrue(RuleMatcher.matches(rule, incomingSender, incomingContent))

        val rendered = RuleMatcher.renderOutput(dest, incomingSender, incomingContent)
        assertTrue(rendered.contains("\"form\":\"+989999987641\""))
        assertTrue(rendered.contains("\"content\":\"بلو\\nواریز پول\\nمحمدحسین عزیز، 1,952,900 ریال به حساب شما نشست.\""))
    }

    @Test
    fun scenarioB_PasargadToSms_matchesAndRendersText() {
        val dest = RuleDestination(
            destinationType = DestinationType.SMS,
            destinationAddress = "09120000000",
            outgoingSlotIndex = 0,
            smsTemplate = "پیام پاسارگاد: {{content}}"
        )
        val rule = Rule(
            id = 2,
            name = "پاسارگاد به SMS",
            enabled = true,
            sender = "B.Pasargad",
            containsText = "+",
            destinations = listOf(dest)
        )

        val incomingSender = "B.PASARGAD"
        val incomingContent = "+1,400,000 ریال واریز شد"

        assertTrue(RuleMatcher.matches(rule, incomingSender, incomingContent))

        val rendered = RuleMatcher.renderOutput(dest, incomingSender, incomingContent)
        assertEquals("پیام پاسارگاد: +1,400,000 ریال واریز شد", rendered)
    }

    @Test
    fun multiDestinationRule_rendersEachDestinationIndependently() {
        val destUrl = RuleDestination(
            destinationType = DestinationType.URL,
            destinationAddress = "https://server.com/api",
            jsonFields = mapOf("s" to "{{sender}}", "c" to "{{content}}")
        )
        val destSms = RuleDestination(
            destinationType = DestinationType.SMS,
            destinationAddress = "09121111111",
            smsTemplate = "انتقال: {{content}}"
        )
        val rule = Rule(
            id = 3,
            name = "تست چند مقصدی",
            enabled = true,
            sender = "*",
            destinations = listOf(destUrl, destSms)
        )

        val sender = "09129999999"
        val content = "تست پیامک"

        assertTrue(RuleMatcher.matches(rule, sender, content))
        assertEquals(2, rule.destinations.size)

        val renderedUrl = RuleMatcher.renderOutput(rule.destinations[0], sender, content)
        val renderedSms = RuleMatcher.renderOutput(rule.destinations[1], sender, content)

        assertTrue(renderedUrl.contains("\"s\":\"09129999999\""))
        assertEquals("انتقال: تست پیامک", renderedSms)
    }

    @Test
    fun mismatchSender_returnsFalse() {
        val rule = Rule(
            id = 4,
            name = "تست",
            enabled = true,
            sender = "09121111111",
            destinations = listOf(RuleDestination(destinationAddress = "09122222222"))
        )

        assertFalse(RuleMatcher.matches(rule, "09129999999", "هر متنی"))
    }
}
