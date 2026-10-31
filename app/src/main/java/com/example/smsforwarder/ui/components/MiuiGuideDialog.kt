package com.example.smsforwarder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiuiGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("راهنمای پس‌زمینه شیائومی (MIUI)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "در گوشی‌های شیائومی برای دریافت و ارسال پایدار پیامک‌ها در پس‌زمینه (حتی هنگام بستن اپلیکیشن) مراحل زیر را انجام دهید:",
                    fontSize = 13.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("۱. فعال‌سازی راه اندازی خودکار (Background Autostart):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("مسیر: تنظیمات -> برنامه‌ها -> مجوزها -> راه‌اندازی خودکار در پس‌زمینه (فعال کردن SmsForwarder)", fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("۲. بهینه‌سازی باتری (Battery Saver):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("مسیر: تنظیمات -> برنامه‌ها -> مدیریت برنامه‌ها -> SmsForwarder -> موفقیب باتری -> انتخاب «بدون محدودیت» (No restrictions)", fontSize = 12.sp)
                    }
                }

                Text(
                    text = "نکته: در صورت Force Stop کردن اپلیکیشن از تنظیمات، سیستم‌عامل تا بازکردن مجدد اپ، دریافت پیامک را متوقف خواهد کرد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("متوجه شدم")
            }
        }
    )
}