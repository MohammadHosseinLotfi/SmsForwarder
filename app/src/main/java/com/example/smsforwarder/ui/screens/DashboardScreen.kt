package com.example.smsforwarder.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.DeliveryState
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.ui.components.*
import com.example.smsforwarder.ui.theme.*
import com.example.smsforwarder.ui.util.*

@Composable
fun DashboardScreen(
    rules: List<RuleEntity>,
    deliveries: List<DeliveryEntity>,
    isServiceEnabled: Boolean = true,
    onToggleServiceEnabled: (Boolean) -> Unit = {},
    onToggleRule: (RuleEntity) -> Unit,
    onEditRule: (RuleEntity) -> Unit,
    onNavigateToLogsWithFilter: (LogFilter) -> Unit,
    onShowMiuiGuide: () -> Unit
) {
    val today = remember { timestampToJalali(System.currentTimeMillis()) }

    val (succeededTodayCount, pendingTodayCount, errorTodayCount) = remember(deliveries, today) {
        var succ = 0
        var pend = 0
        var err = 0
        for (delivery in deliveries) {
            val jalali = timestampToJalali(delivery.receivedAt)
            if (jalali.isSameDay(today.year, today.month, today.day)) {
                when (delivery.state) {
                    DeliveryState.SUCCEEDED -> succ++
                    DeliveryState.QUEUED, DeliveryState.WAITING_NETWORK, DeliveryState.IN_FLIGHT, DeliveryState.RETRY_WAIT -> pend++
                    DeliveryState.BLOCKED, DeliveryState.NEEDS_REVIEW, DeliveryState.UNKNOWN, DeliveryState.CANCELLED -> err++
                }
            }
        }
        Triple(succ, pend, err)
    }

    val activeRuleCount = remember(rules) { rules.count { it.enabled } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        KashiPageHeader(title = "داشبورد")

        Spacer(modifier = Modifier.height(4.dp))

        // Master Service Control Switch Card
        ServiceMasterSwitchCard(
            isServiceEnabled = isServiceEnabled,
            onToggleService = onToggleServiceEnabled
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Today's Delivery Statistics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardStatCard(
                title = "موفق",
                count = succeededTodayCount,
                icon = KashiIcons.Success,
                accentColor = KashiSuccessGreen,
                iconContainerColor = KashiSuccessContainer,
                onClick = { onNavigateToLogsWithFilter(LogFilter.SUCCEEDED) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "در صف",
                count = pendingTodayCount,
                icon = KashiIcons.Clock,
                accentColor = KashiWarningAmber,
                iconContainerColor = KashiWarningContainer,
                onClick = { onNavigateToLogsWithFilter(LogFilter.PENDING) },
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "خطادار",
                count = errorTodayCount,
                icon = KashiIcons.Error,
                accentColor = KashiErrorRose,
                iconContainerColor = KashiErrorContainer,
                onClick = { onNavigateToLogsWithFilter(LogFilter.ERROR) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Background Persistence Guide Banner
        KashiBanner(
            title = "راهنمای فعالیت پایدار در پس‌زمینه",
            description = "تنظیمات ضروری باتری برای شیائومی، سامسونگ و سایر گوشی‌ها جهت دریافت دائمی پیامک.",
            icon = KashiIcons.Phone,
            actionText = "راهنما",
            onActionClick = onShowMiuiGuide
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Forwarding Rules Summary Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "قوانین انتقال",
                fontFamily = VazirmatnFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Text(
                    text = "${activeRuleCount.toPersianDigits()} فعال",
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (rules.isEmpty()) {
            KashiEmptyState(
                icon = KashiIcons.Workflow,
                title = "هیچ قانونی تعریف نشده است",
                description = "جهت ثبت قانون جدید به تب «قوانین انتقال» مراجعه کنید."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp)
            ) {
                items(rules, key = { it.id }, contentType = { "compact_rule_card" }) { rule ->
                    CompactRuleCard(
                        rule = rule,
                        onToggle = { onToggleRule(rule) },
                        onClick = { onEditRule(rule) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceMasterSwitchCard(
    isServiceEnabled: Boolean,
    onToggleService: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isServiceEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(220),
        label = "service_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isServiceEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
        animationSpec = tween(220),
        label = "service_card_border"
    )
    val iconBoxBg by animateColorAsState(
        targetValue = if (isServiceEnabled) KashiSuccessContainer else KashiErrorContainer,
        animationSpec = tween(220),
        label = "service_icon_bg"
    )
    val accentColor by animateColorAsState(
        targetValue = if (isServiceEnabled) KashiSuccessGreen else KashiErrorRose,
        animationSpec = tween(220),
        label = "service_accent"
    )

    Surface(
        onClick = { onToggleService(!isServiceEnabled) },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = if (isServiceEnabled) 2.dp else 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBoxBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isServiceEnabled) KashiIcons.Workflow else KashiIcons.XCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isServiceEnabled) "سرویس انتقال پیامک فعال است" else "سرویس انتقال پیامک متوقف است",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isServiceEnabled) {
                            "دریافت و هدایت خودکار پیامک‌ها بر اساس قوانین فعال"
                        } else {
                            "هیچ پیامکی خوانده یا ارسال نمی‌شود (توقف کامل)"
                        },
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isServiceEnabled) MaterialTheme.colorScheme.onSurfaceVariant else KashiErrorRose
                    )
                }
            }

            Switch(
                checked = isServiceEnabled,
                onCheckedChange = { onToggleService(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    iconContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontFamily = VazirmatnFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = count.toPersianDigits(),
                fontFamily = VazirmatnFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

@Composable
private fun CompactRuleCard(
    rule: RuleEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val isAllSenders = rule.sender.isBlank() || rule.sender.trim() == "*"
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary
    val senderBadge = remember(rule.sender, secondaryColor, onSecondaryColor) {
        if (isAllSenders) {
            SenderBadgeInfo("همه فرستنده‌ها", secondaryColor, onSecondaryColor)
        } else {
            detectSenderBadge(rule.sender)
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (rule.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = rule.name,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (rule.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                senderBadge?.let { badge ->
                    Surface(
                        color = badge.backgroundColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge.label,
                            fontFamily = VazirmatnFontFamily,
                            color = badge.textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!rule.containsText.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = KashiIcons.Filter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "«${rule.containsText.toPersianDigits()}»",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 95.dp)
                            )
                        }
                    }
                }
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.scale(0.75f)
            )
        }
    }
}
