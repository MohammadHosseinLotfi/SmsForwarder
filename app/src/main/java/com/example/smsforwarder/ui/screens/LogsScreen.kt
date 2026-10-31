package com.example.smsforwarder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.DeliveryState
import com.example.smsforwarder.rules.DestinationType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    deliveries: List<DeliveryEntity>,
    onCancelDelivery: (Long) -> Unit,
    onClearAllLogs: () -> Unit
) {
    var selectedDelivery by remember { mutableStateOf<DeliveryEntity?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("پاک‌سازی تاریخچه") },
            text = { Text("آیا از حذف تمام گزارشات ارسال اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirm = false
                        onClearAllLogs()
                    }
                ) {
                    Text("حذف همه", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    selectedDelivery?.let { delivery ->
        AlertDialog(
            onDismissRequest = { selectedDelivery = null },
            title = { Text("جزئیات ارسال") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نام قانون: ${delivery.ruleNameSnapshot}", fontWeight = FontWeight.Bold)
                    Text("فرستنده: ${delivery.senderRaw}")
                    Text("مقصد: ${delivery.destinationType} (${delivery.destinationAddress})")
                    if (delivery.destinationType == DestinationType.SMS) {
                        Text("سیم‌کارت: سیم ${delivery.outgoingSlotIndex + 1}")
                    }
                    Text("تعداد تلاش‌ها: ${delivery.attemptCount}")
                    delivery.errorMessage?.let { err ->
                        Text("علت خطا: $err", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("بدنه ارسال‌شده (Payload):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = delivery.renderedPayload,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDelivery = null }) {
                    Text("بستن")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گزارشات و صف ارسال", fontWeight = FontWeight.Bold) },
                actions = {
                    if (deliveries.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "پاک‌سازی تاریخچه",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (deliveries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "تاریخچه ارسال خالی است",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(deliveries, key = { it.deliveryId }) { delivery ->
                        LogCard(
                            delivery = delivery,
                            onShowDetails = { selectedDelivery = delivery },
                            onCancel = { onCancelDelivery(delivery.deliveryId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(
    delivery: DeliveryEntity,
    onShowDetails: () -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss - yyyy/MM/dd", Locale.getDefault()) }
    val formattedTime = remember(delivery.receivedAt) { dateFormat.format(Date(delivery.receivedAt)) }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        onClick = onShowDetails,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = delivery.ruleNameSnapshot,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                StateBadge(state = delivery.state)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "فرستنده: ${delivery.senderRaw} | زمان: $formattedTime",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مقصد: ${delivery.destinationAddress}",
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (delivery.state == DeliveryState.QUEUED || delivery.state == DeliveryState.WAITING_NETWORK || delivery.state == DeliveryState.RETRY_WAIT) {
                    TextButton(onClick = onCancel) {
                        Text("لغو", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (!delivery.errorMessage.isNullOrBlank()) {
                Text(
                    text = "خطا: ${delivery.errorMessage}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StateBadge(state: DeliveryState) {
    val (bgColor, textColor, label) = when (state) {
        DeliveryState.SUCCEEDED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "موفق")
        DeliveryState.QUEUED -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "در صف")
        DeliveryState.WAITING_NETWORK -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "منتظر شبکه")
        DeliveryState.IN_FLIGHT -> Triple(Color(0xFFE1F5FE), Color(0xFF0288D1), "در حال ارسال")
        DeliveryState.RETRY_WAIT -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "منتظر تلاش مجدد")
        DeliveryState.BLOCKED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "مسدود (خطای مجوز/سیم)")
        DeliveryState.NEEDS_REVIEW -> Triple(Color(0xFFFFEBEE), Color(0xFFB71C1C), "نیازمند بررسی")
        DeliveryState.UNKNOWN -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "نامشخص")
        DeliveryState.CANCELLED -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), "لغو شده")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}