package com.example.smsforwarder.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.ui.theme.KashiWarningContainer
import com.example.smsforwarder.ui.theme.VazirmatnFontFamily

@Composable
fun MiuiGuideDialog(
    onDismiss: () -> Unit
) {
    val initialTab = remember {
        val m = Build.MANUFACTURER.lowercase()
        when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> 0
            m.contains("samsung") -> 1
            else -> 2
        }
    }

    var selectedTab by remember { mutableIntStateOf(initialTab) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = KashiIcons.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "راهنمای فعالیت پایدار در پس‌زمینه",
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تنظیمات ضروری باتری جهت دریافت همیشگی پیامک‌ها",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KashiSegmentedControl(
                    items = listOf("شیائومی", "سامسونگ", "سایر گوشی‌ها"),
                    selectedIndex = selectedTab,
                    onItemSelected = { selectedTab = it }
                )

                when (selectedTab) {
                    0 -> { // Xiaomi
                        StepCard(
                            stepNumber = "۰۱",
                            title = "راه‌اندازی خودکار (Autostart)",
                            description = "تنظیمات ──> برنامه‌ها ──> مجوزها ──> راه‌اندازی خودکار در پس‌زمینه ──> فعال‌سازی SmsForwarder",
                            icon = KashiIcons.Workflow
                        )
                        StepCard(
                            stepNumber = "۰۲",
                            title = "مدیریت باتری (No restrictions)",
                            description = "تنظیمات ──> برنامه‌ها ──> مدیریت برنامه‌ها ──> SmsForwarder ──> مدیریت باتری ──> انتخاب «بدون محدودیت»",
                            icon = KashiIcons.Info
                        )
                    }
                    1 -> { // Samsung
                        StepCard(
                            stepNumber = "۰۱",
                            title = "باتری نامحدود (Unrestricted)",
                            description = "تنظیمات ──> برنامه‌ها ──> SmsForwarder ──> باتری ──> انتخاب گزینه «نامحدود» (Unrestricted)",
                            icon = KashiIcons.Info
                        )
                        StepCard(
                            stepNumber = "۰۲",
                            title = "عدم خواب عمیق (Never Sleeping)",
                            description = "تنظیمات ──> مراقبت از دستگاه (Device Care) ──> باتری ──> محدودیت‌های پس‌زمینه ──> افزودن اپ به «برنامه‌هایی که هرگز به خواب نمی‌روند»",
                            icon = KashiIcons.Workflow
                        )
                    }
                    else -> { // Other brands
                        StepCard(
                            stepNumber = "۰۱",
                            title = "غیرفعال‌سازی بهینه‌سازی باتری",
                            description = "تنظیمات ──> برنامه‌ها ──> SmsForwarder ──> باتری ──> عدم بهینه‌سازی (Don't optimize / Unrestricted)",
                            icon = KashiIcons.Info
                        )
                        StepCard(
                            stepNumber = "۰۲",
                            title = "اجرای خودکار (هواوی/آنر/سایر)",
                            description = "تنظیمات ──> باتری ──> راه‌اندازی برنامه (App Launch) ──> مدیریت دستی و فعال‌سازی هر سه گزینه",
                            icon = KashiIcons.Workflow
                        )
                    }
                }

                // Caution Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KashiWarningContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = KashiIcons.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "نکته: در صورت بستن اجباری (Force Stop) برنامه از تنظیمات، تا بازکردن مجدد اپ، دریافت پیامک متوقف خواهد شد.",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("متوجه شدم", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = null
    )
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    fontFamily = VazirmatnFontFamily,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
