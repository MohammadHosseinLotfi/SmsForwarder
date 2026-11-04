package com.example.smsforwarder.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.DeliveryState
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.ui.components.*
import com.example.smsforwarder.ui.theme.*
import com.example.smsforwarder.ui.util.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LogFilter {
    ALL,
    SUCCEEDED,
    PENDING,
    ERROR
}

@Composable
fun rememberSheetOverscrollBlocker(): NestedScrollConnection = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return available
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ): Velocity {
            return available
        }
    }
}

@Composable
fun LogsScreen(
    deliveries: List<DeliveryEntity>,
    rules: List<RuleEntity>,
    initialFilter: LogFilter = LogFilter.ALL,
    initialFilterToday: Boolean = false,
    onFilterConsumed: () -> Unit = {},
    onCancelDelivery: (Long) -> Unit,
    onResendDelivery: (Long) -> Unit,
    onClearAllLogs: () -> Unit
) {
    var selectedDelivery by remember { mutableStateOf<DeliveryEntity?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(initialFilter) }

    var selectedDate by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedRuleName by remember { mutableStateOf<String?>(null) }
    var showRuleDropdown by remember { mutableStateOf(false) }

    val distinctRuleNames = remember(rules) { rules.map { it.name }.distinct() }

    LaunchedEffect(initialFilter, initialFilterToday) {
        activeFilter = initialFilter
        if (initialFilterToday) {
            val t = timestampToJalali(System.currentTimeMillis())
            selectedDate = Triple(t.year, t.month, t.day)
            onFilterConsumed()
        }
    }

    val (succeededCount, pendingCount, errorCount) = remember(deliveries) {
        var succ = 0
        var pend = 0
        var err = 0
        for (d in deliveries) {
            when (d.state) {
                DeliveryState.SUCCEEDED -> succ++
                DeliveryState.QUEUED, DeliveryState.WAITING_NETWORK, DeliveryState.IN_FLIGHT, DeliveryState.RETRY_WAIT -> pend++
                DeliveryState.BLOCKED, DeliveryState.NEEDS_REVIEW, DeliveryState.UNKNOWN, DeliveryState.CANCELLED -> err++
            }
        }
        Triple(succ, pend, err)
    }

    val filteredDeliveries = remember(deliveries, activeFilter, selectedDate, selectedRuleName) {
        deliveries.filter { delivery ->
            val matchFilter = when (activeFilter) {
                LogFilter.ALL -> true
                LogFilter.SUCCEEDED -> delivery.state == DeliveryState.SUCCEEDED
                LogFilter.PENDING -> delivery.state == DeliveryState.QUEUED || delivery.state == DeliveryState.WAITING_NETWORK ||
                        delivery.state == DeliveryState.IN_FLIGHT || delivery.state == DeliveryState.RETRY_WAIT
                LogFilter.ERROR -> delivery.state == DeliveryState.BLOCKED || delivery.state == DeliveryState.NEEDS_REVIEW ||
                        delivery.state == DeliveryState.UNKNOWN || delivery.state == DeliveryState.CANCELLED
            }

            val matchDate = if (selectedDate == null) {
                true
            } else {
                val (y, m, d) = selectedDate!!
                timestampToJalali(delivery.receivedAt).isSameDay(y, m, d)
            }

            val matchRule = selectedRuleName == null || delivery.ruleNameSnapshot == selectedRuleName

            matchFilter && matchDate && matchRule
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "پاک‌سازی تاریخچه",
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف تمام گزارشات ارسال اطمینان دارید؟",
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showClearAllConfirm = false
                            onClearAllLogs()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("حذف همه", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showClearAllConfirm = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("انصراف", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showDatePicker) {
        KashiJalaliDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { y, m, d -> selectedDate = Triple(y, m, d) },
            onDismiss = { showDatePicker = false }
        )
    }

    selectedDelivery?.let { selected ->
        val siblingDeliveries = remember(deliveries, selected) {
            deliveries.filter {
                it.ruleId == selected.ruleId &&
                it.senderRaw == selected.senderRaw &&
                it.contentRaw == selected.contentRaw &&
                abs(it.receivedAt - selected.receivedAt) < 2000L
            }.sortedBy { it.deliveryId }
        }.ifEmpty { listOf(selected) }

        val livePrimary = deliveries.firstOrNull { it.deliveryId == selected.deliveryId } ?: selected

        KashiReceiptDialog(
            delivery = livePrimary,
            siblingDeliveries = siblingDeliveries,
            onCancelDelivery = onCancelDelivery,
            onResendDelivery = onResendDelivery,
            onDismiss = { selectedDelivery = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            KashiPageHeader(
                title = "گزارشات ارسال",
                actions = {
                    if (deliveries.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(
                                imageVector = KashiIcons.Trash,
                                contentDescription = "پاک‌سازی تاریخچه",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )

            // Status Filter Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KashiFilterCard(
                    title = "همه",
                    count = deliveries.size,
                    icon = KashiIcons.Layers,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isSelected = activeFilter == LogFilter.ALL,
                    onClick = { activeFilter = LogFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
                KashiFilterCard(
                    title = "موفق",
                    count = succeededCount,
                    icon = KashiIcons.Success,
                    accentColor = KashiSuccessGreen,
                    isSelected = activeFilter == LogFilter.SUCCEEDED,
                    onClick = { activeFilter = LogFilter.SUCCEEDED },
                    modifier = Modifier.weight(1f)
                )
                KashiFilterCard(
                    title = "در صف",
                    count = pendingCount,
                    icon = KashiIcons.Clock,
                    accentColor = KashiWarningAmber,
                    isSelected = activeFilter == LogFilter.PENDING,
                    onClick = { activeFilter = LogFilter.PENDING },
                    modifier = Modifier.weight(1f)
                )
                KashiFilterCard(
                    title = "خطادار",
                    count = errorCount,
                    icon = KashiIcons.Error,
                    accentColor = KashiErrorRose,
                    isSelected = activeFilter == LogFilter.ERROR,
                    onClick = { activeFilter = LogFilter.ERROR },
                    modifier = Modifier.weight(1f)
                )
            }

            // Side-by-Side Date Filter & Rule Dropdown Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = KashiIcons.Calendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedDate == null) {
                                    "همه تاریخ‌ها"
                                } else {
                                    val (_, m, d) = selectedDate!!
                                    "${d.toPersianDigits()} ${getPersianMonthName(m)}"
                                },
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selectedDate == null) {
                            Surface(
                                onClick = {
                                    val t = timestampToJalali(System.currentTimeMillis())
                                    selectedDate = Triple(t.year, t.month, t.day)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "امروز",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { selectedDate = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = KashiIcons.XCircle,
                                    contentDescription = "پاک‌سازی تاریخ",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        onClick = { showRuleDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Workflow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedRuleName ?: "همه قوانین",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Icon(
                                imageVector = KashiIcons.ChevronLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(-90f)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showRuleDropdown,
                        onDismissRequest = { showRuleDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "همه قوانین",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedRuleName == null) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedRuleName = null
                                showRuleDropdown = false
                            }
                        )
                        distinctRuleNames.forEach { ruleName ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = ruleName,
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedRuleName == ruleName) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedRuleName = ruleName
                                    showRuleDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            if (filteredDeliveries.isEmpty()) {
                KashiEmptyState(
                    icon = KashiIcons.Receipt,
                    title = "هیچ گزارشی یافت نشد",
                    description = if (deliveries.isEmpty()) "تاریخچه ارسال پیامک‌ها خالی است." else "گزارشی مطابق با فیلترها یافت نشد."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 84.dp)
                ) {
                    items(filteredDeliveries, key = { it.deliveryId }, contentType = { "log_card" }) { delivery ->
                        Box(modifier = Modifier.animateItem()) {
                            LogCard(
                                delivery = delivery,
                                onShowDetails = { selectedDelivery = delivery }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KashiFilterCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = count.toPersianDigits(),
                fontFamily = VazirmatnFontFamily,
                fontSize = 20.sp,
                color = accentColor
            )
        }
    }
}

@Composable
fun LogCard(
    delivery: DeliveryEntity,
    onShowDetails: () -> Unit
) {
    val jalali = remember(delivery.receivedAt) { timestampToJalali(delivery.receivedAt) }
    val amount = remember(delivery.renderedPayload) { extractMoneyAmount(delivery.renderedPayload) }

    val (bgColor, iconTint, icon) = when (delivery.state) {
        DeliveryState.SUCCEEDED -> Triple(KashiSuccessContainer, KashiSuccessGreen, KashiIcons.Success)
        DeliveryState.QUEUED, DeliveryState.WAITING_NETWORK, DeliveryState.IN_FLIGHT, DeliveryState.RETRY_WAIT ->
            Triple(KashiWarningContainer, KashiWarningAmber, KashiIcons.Clock)
        else -> Triple(KashiErrorContainer, KashiErrorRose, KashiIcons.Error)
    }

    Surface(
        onClick = onShowDetails,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = jalali.formattedTimeShort,
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = delivery.ruleNameSnapshot,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                amount?.let { money ->
                    Text(
                        text = money,
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StateBadge(state: DeliveryState) {
    val (bgColor, textColor, label) = when (state) {
        DeliveryState.SUCCEEDED -> Triple(KashiSuccessContainer, Color(0xFF065F46), "موفق")
        DeliveryState.QUEUED -> Triple(KashiWarningContainer, Color(0xFF92400E), "در صف")
        DeliveryState.WAITING_NETWORK -> Triple(KashiInfoContainer, Color(0xFF1E40AF), "منتظر شبکه")
        DeliveryState.IN_FLIGHT -> Triple(KashiInfoContainer, Color(0xFF1E40AF), "در حال ارسال")
        DeliveryState.RETRY_WAIT -> Triple(KashiWarningContainer, Color(0xFF92400E), "منتظر تلاش مجدد")
        DeliveryState.BLOCKED -> Triple(KashiErrorContainer, Color(0xFF991B1B), "مسدود")
        DeliveryState.NEEDS_REVIEW -> Triple(KashiErrorContainer, Color(0xFF991B1B), "نیازمند بررسی")
        DeliveryState.UNKNOWN -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "نامشخص")
        DeliveryState.CANCELLED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "لغو شده")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                fontFamily = VazirmatnFontFamily,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val ResendableStates = setOf(
    DeliveryState.QUEUED,
    DeliveryState.WAITING_NETWORK,
    DeliveryState.RETRY_WAIT,
    DeliveryState.BLOCKED,
    DeliveryState.NEEDS_REVIEW,
    DeliveryState.UNKNOWN,
    DeliveryState.CANCELLED
)

private val CancellableStates = setOf(
    DeliveryState.QUEUED,
    DeliveryState.WAITING_NETWORK,
    DeliveryState.RETRY_WAIT
)

private data class DestinationStatusUi(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val container: Color
)

private fun DeliveryState.toDestinationStatusUi(): DestinationStatusUi = when (this) {
    DeliveryState.SUCCEEDED ->
        DestinationStatusUi("رسید", KashiIcons.Success, KashiSuccessGreen, KashiSuccessContainer)

    DeliveryState.QUEUED,
    DeliveryState.WAITING_NETWORK,
    DeliveryState.IN_FLIGHT,
    DeliveryState.RETRY_WAIT ->
        DestinationStatusUi("در صف", KashiIcons.Clock, KashiWarningAmber, KashiWarningContainer)

    DeliveryState.BLOCKED,
    DeliveryState.NEEDS_REVIEW,
    DeliveryState.UNKNOWN,
    DeliveryState.CANCELLED ->
        DestinationStatusUi("ناموفق", KashiIcons.Error, KashiErrorRose, KashiErrorContainer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KashiReceiptDialog(
    delivery: DeliveryEntity,
    siblingDeliveries: List<DeliveryEntity> = listOf(delivery),
    onCancelDelivery: (Long) -> Unit,
    onResendDelivery: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded }
    )

    var selectedDetailsDelivery by remember { mutableStateOf<DeliveryEntity?>(null) }

    val jalali = remember(delivery.receivedAt) { timestampToJalali(delivery.receivedAt) }
    val amount = remember(delivery.renderedPayload) { extractMoneyAmount(delivery.renderedPayload) }

    val isAllSenders = delivery.senderRaw.isBlank() || delivery.senderRaw.trim() == "*"
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary
    val senderBadge = remember(delivery.senderRaw, secondaryColor, onSecondaryColor) {
        if (isAllSenders) {
            SenderBadgeInfo("همه فرستنده‌ها", secondaryColor, onSecondaryColor)
        } else {
            detectSenderBadge(delivery.senderRaw)
        }
    }

    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    val overscrollBlocker = rememberSheetOverscrollBlocker()

    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            sheetMaxWidth = Dp.Unspecified,
            dragHandle = { KashiDragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .nestedScroll(overscrollBlocker)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header + Status Badge
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReceiptSheetHeader(title = "جزئیات ارسال", onClose = closeSheet)
                    StateBadge(state = delivery.state)
                }

                // Basic metadata
                ReceiptCard {
                    ReceiptRow(label = "نام قانون", value = delivery.ruleNameSnapshot)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فرستنده",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isAllSenders) "همه فرستنده‌ها" else delivery.senderRaw.toPersianDigits(),
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            senderBadge?.let { badge ->
                                Surface(color = badge.backgroundColor, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = badge.label,
                                        fontFamily = VazirmatnFontFamily,
                                        color = badge.textColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    ReceiptRow(label = "زمان دریافت", value = jalali.fullFormatted)
                    ReceiptRow(label = "تعداد تلاش‌ها", value = delivery.attemptCount.toPersianDigits())

                    amount?.let { money ->
                        ReceiptRow(
                            label = "مبلغ شناسایی‌شده",
                            value = money,
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Destination section
                val destSectionTitle = if (siblingDeliveries.size > 1) {
                    "ارسال به مقصدها (${siblingDeliveries.size.toPersianDigits()} مقصد)"
                } else {
                    "ارسال به مقصد"
                }

                ReceiptSectionTitle(destSectionTitle)

                siblingDeliveries.forEachIndexed { index, itemDelivery ->
                    val itemIsUrl = itemDelivery.destinationType == DestinationType.URL
                    val itemStatus = remember(itemDelivery.state) { itemDelivery.state.toDestinationStatusUi() }
                    val itemSentJalaliText = remember(itemDelivery.lastAttemptAt) {
                        itemDelivery.lastAttemptAt?.let { timestampToJalali(it).fullFormatted } ?: "هنوز ارسال نشده"
                    }

                    var resendRequested by remember(itemDelivery.state, itemDelivery.attemptCount) { mutableStateOf(false) }
                    LaunchedEffect(resendRequested) {
                        if (resendRequested) {
                            delay(5_000)
                            resendRequested = false
                        }
                    }

                    ReceiptCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(itemStatus.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = itemStatus.icon,
                                        contentDescription = null,
                                        tint = itemStatus.tint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val cardTitle = if (siblingDeliveries.size > 1) {
                                        "مقصد ${(index + 1).toPersianDigits()} (${if (itemIsUrl) "وب‌هوک" else "پیامک"})"
                                    } else {
                                        "مقصد"
                                    }
                                    Text(
                                        text = cardTitle,
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (itemIsUrl) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = itemDelivery.destinationAddress,
                                                fontFamily = VazirmatnFontFamily,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = itemDelivery.destinationAddress.toPersianDigits(),
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemDelivery.httpStatusCode?.let { code ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = code.toPersianDigits(),
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = itemStatus.label,
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = itemStatus.tint
                                )
                            }
                        }

                        // transmission date & time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = KashiIcons.Calendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = itemSentJalaliText,
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!itemDelivery.errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = KashiErrorContainer,
                                border = BorderStroke(1.dp, KashiErrorRose.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "علت خطا: ${itemDelivery.errorMessage}",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 12.sp,
                                    color = KashiErrorRose,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // «مشاهده جزئیات» Button
                        OutlinedButton(
                            onClick = { selectedDetailsDelivery = itemDelivery },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (itemIsUrl) KashiIcons.Webhook else KashiIcons.ChatSquare,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "مشاهده جزئیات",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // «ارسال الان» Button
                        if (itemDelivery.state in ResendableStates) {
                            Button(
                                onClick = {
                                    resendRequested = true
                                    onResendDelivery(itemDelivery.deliveryId)
                                    Toast.makeText(context, "درخواست ارسال مجدد ثبت شد", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !resendRequested,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (resendRequested) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = LocalContentColor.current
                                    )
                                } else {
                                    Text(
                                        "ارسال الان",
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Cancel Button
                        if (itemDelivery.state in CancellableStates) {
                            OutlinedButton(
                                onClick = {
                                    onCancelDelivery(itemDelivery.deliveryId)
                                    closeSheet()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(KashiIcons.XCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "لغو این ارسال در صف",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // SMS text section
                ReceiptSectionTitle("متن پیامک")

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = delivery.contentRaw,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(delivery.contentRaw))
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Toast.makeText(context, "متن پیامک کپی شد", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(KashiIcons.Copy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کپی متن", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = closeSheet,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("بستن", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        selectedDetailsDelivery?.let { detailsDelivery ->
            KashiRequestDetailsSheet(
                delivery = detailsDelivery,
                maxSheetHeight = maxSheetHeight,
                onDismiss = { selectedDetailsDelivery = null }
            )
        }
    }
}

/** Second sheet: what exactly was sent to the destination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KashiRequestDetailsSheet(
    delivery: DeliveryEntity,
    maxSheetHeight: Dp,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded }
    )

    val isUrl = delivery.destinationType == DestinationType.URL
    val sentJalaliText = remember(delivery.lastAttemptAt) {
        delivery.lastAttemptAt?.let { timestampToJalali(it).fullFormatted } ?: "هنوز ارسال نشده"
    }
    val overscrollBlocker = rememberSheetOverscrollBlocker()

    val formattedPayload = remember(delivery.renderedPayload, isUrl) {
        formatAndHighlightPayload(delivery.renderedPayload, isUrl)
    }

    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        dragHandle = { KashiDragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .nestedScroll(overscrollBlocker)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReceiptSheetHeader(title = "جزئیات ارسال به مقصد", onClose = closeSheet)

            ReceiptCard {
                RequestDetailField(
                    label = "نوع مقصد",
                    value = if (isUrl) "وب‌هوک URL"
                    else "پیامک SMS (سیم‌کارت ${(delivery.outgoingSlotIndex + 1).toPersianDigits()})"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                RequestDetailField(
                    label = if (isUrl) "نشانی مقصد" else "شماره مقصد",
                    value = if (isUrl) delivery.destinationAddress else delivery.destinationAddress.toPersianDigits(),
                    ltr = isUrl
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                RequestDetailField(label = "زمان ارسال", value = sentJalaliText)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بدنه ارسال‌شده",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(delivery.renderedPayload))
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Toast.makeText(context, "بدنه ارسال‌شده کپی شد", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = KashiIcons.Copy,
                            contentDescription = "کپی بدنه",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(KashiDeepSlate)
                        .padding(12.dp)
                ) {
                    if (isUrl) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            SelectionContainer {
                                Text(
                                    text = formattedPayload,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    softWrap = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            SelectionContainer {
                                Text(
                                    text = delivery.renderedPayload,
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp,
                                    fontFamily = VazirmatnFontFamily,
                                    color = KashiSkyTurquoise,
                                    softWrap = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = closeSheet,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("بستن", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatAndHighlightPayload(rawPayload: String, isJson: Boolean): AnnotatedString {
    val trimmed = rawPayload.trim()
    val shouldPrettyJson = isJson || trimmed.startsWith("{") || trimmed.startsWith("[")

    if (shouldPrettyJson) {
        val formattedJson = try {
            val jsonElement = JsonParser.parseString(trimmed)
            GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonElement)
        } catch (_: Exception) {
            rawPayload
        }
        return highlightJsonSyntax(formattedJson)
    }

    return AnnotatedString(rawPayload)
}

private fun highlightJsonSyntax(json: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val regex = Regex("""("(?:\\.|[^"\\])*")|(\b\d+\b|true|false|null)|([{}:,\[\]])""")
    var lastIndex = 0

    for (match in regex.findAll(json)) {
        if (match.range.first > lastIndex) {
            builder.append(json.substring(lastIndex, match.range.first))
        }
        val text = match.value
        val keyOrString = match.groups[1]
        val numberOrBool = match.groups[2]
        val symbol = match.groups[3]

        when {
            keyOrString != null -> {
                val remaining = json.substring(match.range.last + 1).trimStart()
                if (remaining.startsWith(":")) {
                    builder.addStyle(
                        SpanStyle(color = Firoozeh300, fontWeight = FontWeight.Bold),
                        builder.length,
                        builder.length + text.length
                    )
                } else {
                    builder.addStyle(
                        SpanStyle(color = Pesteh400),
                        builder.length,
                        builder.length + text.length
                    )
                }
                builder.append(text)
            }
            numberOrBool != null -> {
                builder.addStyle(
                    SpanStyle(color = Zafaran400),
                    builder.length,
                    builder.length + text.length
                )
                builder.append(text)
            }
            symbol != null -> {
                builder.addStyle(
                    SpanStyle(color = Shab300),
                    builder.length,
                    builder.length + text.length
                )
                builder.append(text)
            }
            else -> builder.append(text)
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < json.length) {
        builder.append(json.substring(lastIndex))
    }
    return builder.toAnnotatedString()
}

@Composable
private fun ReceiptSheetHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = VazirmatnFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = KashiIcons.XCircle,
                contentDescription = "بستن",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ReceiptSectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = VazirmatnFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ReceiptCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun RequestDetailField(
    label: String,
    value: String,
    ltr: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontFamily = VazirmatnFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CompositionLocalProvider(
            LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            SelectionContainer {
                Text(
                    text = value,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = VazirmatnFontFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontFamily = VazirmatnFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
