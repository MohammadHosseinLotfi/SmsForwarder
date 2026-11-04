package com.example.smsforwarder.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.data.getDestinations
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.RuleDestination
import com.example.smsforwarder.ui.components.*
import com.example.smsforwarder.ui.theme.*
import com.example.smsforwarder.ui.util.*
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddEditRuleScreen(
    editingRule: RuleEntity? = null,
    onSaveRule: (RuleEntity) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val gson = remember { Gson() }

    var name by remember { mutableStateOf(editingRule?.name ?: "") }
    var sender by remember { mutableStateOf(editingRule?.sender ?: "") }
    var containsText by remember { mutableStateOf(editingRule?.containsText ?: "") }

    val initialDestinations = remember(editingRule) {
        if (editingRule != null) {
            editingRule.getDestinations(gson)
        } else {
            emptyList()
        }
    }

    var destinations by remember { mutableStateOf(initialDestinations) }

    var showDestinationSheet by remember { mutableStateOf(false) }
    var editingDestination by remember { mutableStateOf<RuleDestination?>(null) }

    val scrollState = rememberScrollState()

    fun buildCurrentEntity(): RuleEntity {
        val primary = destinations.firstOrNull() ?: RuleDestination()
        val primaryJsonMap = primary.jsonFields.filter { it.key.isNotBlank() }
        return RuleEntity(
            id = editingRule?.id ?: 0,
            name = name.trim(),
            enabled = editingRule?.enabled ?: true,
            sender = sender.trim(),
            containsText = containsText.trim().ifBlank { null },
            destinationType = primary.destinationType,
            destinationAddress = primary.destinationAddress.trim(),
            outgoingSlotIndex = primary.outgoingSlotIndex,
            smsTemplate = primary.smsTemplate,
            jsonFieldsJson = gson.toJson(primaryJsonMap),
            destinationsJson = gson.toJson(destinations)
        )
    }

    val isFormValid = name.isNotBlank() && destinations.isNotEmpty() && destinations.all { it.destinationAddress.isNotBlank() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KashiTopBar(
                title = if (editingRule == null) "افزودن قانون جدید" else "ویرایش قانون",
                navigationIcon = KashiIcons.ChevronRight,
                onNavigationClick = onCancel
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = { if (isFormValid) onSaveRule(buildCurrentEntity()) },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(KashiIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ذخیره قانون", fontFamily = VazirmatnFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== بخش ۰۱: مشخصات و فیلتر ====================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(number = "۰۱", title = "مشخصات و فیلتر پیامک")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        KashiPersianInput(
                            value = name,
                            onValueChange = { name = it },
                            label = "نام قانون *",
                            placeholder = "مانند: انتقال واریزهای بلو به سرور",
                            kind = KashiInputKind.TEXT,
                            leadingIcon = KashiIcons.Workflow
                        )

                        KashiPersianInput(
                            value = sender,
                            onValueChange = { sender = it },
                            label = "فرستنده پیامک (اختیاری)",
                            placeholder = "خالی = دریافت از تمام فرستنده‌ها (یا مثلاً 0912...)",
                            kind = KashiInputKind.SENDER,
                            leadingIcon = KashiIcons.Phone
                        )

                        KashiPersianInput(
                            value = containsText,
                            onValueChange = { containsText = it },
                            label = "عبارت داخل متن (اختیاری)",
                            placeholder = "مانند: واریز پول (خالی یعنی تمام پیامک‌های این فرستنده)",
                            kind = KashiInputKind.TEXT,
                            leadingIcon = KashiIcons.Filter
                        )
                    }
                }
            }

            // ==================== بخش ۰۲: مقصدهای ارسال ====================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(number = "۰۲", title = "مقصدهای ارسال")

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${destinations.size.toPersianDigits()} مقصد",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (destinations.isEmpty()) {
                    KashiEmptyState(
                        icon = KashiIcons.Webhook,
                        title = "هیچ مقصدی تعریف نشده است",
                        description = "برای ثبت مقصد ارسال (وب‌هوک یا پیامک)، دکمه «افزودن مقصد جدید» را بزنید.",
                        actionLabel = "افزودن مقصد جدید",
                        onAction = {
                            editingDestination = null
                            showDestinationSheet = true
                        }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        destinations.forEachIndexed { index, destination ->
                            DestinationSummaryCard(
                                destination = destination,
                                index = index,
                                onToggleEnabled = { isEnabled ->
                                    destinations = destinations.map { if (it.id == destination.id) it.copy(enabled = isEnabled) else it }
                                },
                                onEdit = {
                                    editingDestination = destination
                                    showDestinationSheet = true
                                },
                                onDelete = {
                                    destinations = destinations.filter { it.id != destination.id }
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                editingDestination = null
                                showDestinationSheet = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(KashiIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("افزودن مقصد جدید", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDestinationSheet) {
        DestinationFormBottomSheet(
            initialDestination = editingDestination,
            onSaveDestination = { updatedDest ->
                if (editingDestination == null) {
                    destinations = destinations + updatedDest
                } else {
                    destinations = destinations.map { if (it.id == updatedDest.id) updatedDest else it }
                }
                showDestinationSheet = false
                editingDestination = null
            },
            onDismiss = {
                showDestinationSheet = false
                editingDestination = null
            }
        )
    }
}

@Composable
private fun DestinationSummaryCard(
    destination: RuleDestination,
    index: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isUrl = destination.destinationType == DestinationType.URL
    val icon = if (isUrl) KashiIcons.Webhook else KashiIcons.ChatSquare
    val iconBg = if (isUrl) KashiInfoContainer else KashiSuccessContainer
    val iconTint = if (isUrl) KashiInfoBlue else KashiSuccessGreen

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (destination.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (destination.enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Top Row — Type & Enable Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (isUrl) "وب‌هوک اینترنتی (URL)" else "پیامک (SMS)",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = destination.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            // Row 2: Middle Row — Address Box with Label & Copy Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isUrl) "آدرس مقصد" else "شماره گیرنده پیامک",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (isUrl) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = destination.destinationAddress.ifBlank { "آدرس وارد نشده است" },
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (destination.destinationAddress.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                text = destination.destinationAddress.ifBlank { "شماره وارد نشده است" }.toPersianDigits(),
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (destination.destinationAddress.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (destination.destinationAddress.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(destination.destinationAddress))
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        val msg = if (isUrl) "آدرس مقصد کپی شد" else "شماره مقصد کپی شد"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Copy,
                                    contentDescription = "کپی آدرس",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Row 3: Bottom Row — Info Badges on Right, Edit & Delete Buttons on Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Badges (Right / Start in RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isUrl) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${destination.jsonFields.size.toPersianDigits()} فیلد JSON",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = KashiIcons.SimCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "سیم‌کارت ${(destination.outgoingSlotIndex + 1).toPersianDigits()}",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        val operator = remember(destination.destinationAddress) { detectMobileOperator(destination.destinationAddress) }
                        operator?.let { opName ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = opName,
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!destination.enabled) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "غیرفعال",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Edit & Delete Actions (Left / End in RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = KashiIcons.Edit,
                            contentDescription = "ویرایش مقصد",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = KashiIcons.Trash,
                            contentDescription = "حذف مقصد",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationFormBottomSheet(
    initialDestination: RuleDestination?,
    onSaveDestination: (RuleDestination) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded }
    )

    var type by remember { mutableStateOf(initialDestination?.destinationType ?: DestinationType.URL) }
    var address by remember { mutableStateOf(initialDestination?.destinationAddress ?: "") }
    var slotIndex by remember { mutableIntStateOf(initialDestination?.outgoingSlotIndex ?: 0) }
    var smsTemplate by remember { mutableStateOf(initialDestination?.smsTemplate ?: "{{content}}") }

    var jsonFieldsList by remember(initialDestination) {
        val fields = initialDestination?.jsonFields ?: mapOf("sender" to "{{sender}}", "content" to "{{content}}")
        mutableStateOf(fields.map { Pair(it.key, it.value) })
    }

    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    val isSheetValid = address.isNotBlank() && (type == DestinationType.URL || smsTemplate.isNotBlank())
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    val overscrollBlocker = rememberSheetOverscrollBlocker()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            sheetMaxWidth = Dp.Unspecified,
            dragHandle = { KashiDragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .nestedScroll(overscrollBlocker)
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialDestination == null) "افزودن مقصد جدید" else "ویرایش مقصد",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = closeSheet,
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

                // Type Selector
                KashiSegmentedControl(
                    items = listOf("وب‌هوک اینترنتی (URL)", "پیامک (SMS)"),
                    selectedIndex = if (type == DestinationType.URL) 0 else 1,
                    onItemSelected = { index ->
                        type = if (index == 0) DestinationType.URL else DestinationType.SMS
                    },
                    icons = listOf(KashiIcons.Webhook, KashiIcons.ChatSquare)
                )

                if (type == DestinationType.URL) {
                    KashiPersianInput(
                        value = address,
                        onValueChange = { address = it },
                        label = "آدرس URL مقصد (HTTPS) *",
                        placeholder = "https://example.com/api/sms",
                        kind = KashiInputKind.URL,
                        leadingIcon = KashiIcons.Webhook
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فیلدهای JSON سفارشی:",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                jsonFieldsList = jsonFieldsList + Pair("", "{{content}}")
                            }
                        ) {
                            Icon(
                                imageVector = KashiIcons.Plus,
                                contentDescription = "افزودن فیلد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    jsonFieldsList.forEachIndexed { idx, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = pair.first,
                                onValueChange = { newKey ->
                                    val updated = jsonFieldsList.toMutableList()
                                    updated[idx] = Pair(newKey, pair.second)
                                    jsonFieldsList = updated
                                },
                                label = { Text("کلید", fontFamily = VazirmatnFontFamily, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = pair.second,
                                onValueChange = { newVal ->
                                    val updated = jsonFieldsList.toMutableList()
                                    updated[idx] = Pair(pair.first, newVal)
                                    jsonFieldsList = updated
                                },
                                label = { Text("مقدار / متغیر", fontFamily = VazirmatnFontFamily, fontSize = 13.sp) },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (jsonFieldsList.size > 1) {
                                        jsonFieldsList = jsonFieldsList.filterIndexed { i, _ -> i != idx }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Trash,
                                    contentDescription = "حذف فیلد",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("درج متغیر:", fontFamily = VazirmatnFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        VariableBadge(text = "+ {{sender}}") {
                            if (jsonFieldsList.isNotEmpty()) {
                                val last = jsonFieldsList.last()
                                val updated = jsonFieldsList.toMutableList()
                                updated[updated.lastIndex] = Pair(last.first, "${last.second}{{sender}}")
                                jsonFieldsList = updated
                            }
                        }
                        VariableBadge(text = "+ {{content}}") {
                            if (jsonFieldsList.isNotEmpty()) {
                                val last = jsonFieldsList.last()
                                val updated = jsonFieldsList.toMutableList()
                                updated[updated.lastIndex] = Pair(last.first, "${last.second}{{content}}")
                                jsonFieldsList = updated
                            }
                        }
                    }
                } else {
                    KashiPersianInput(
                        value = address,
                        onValueChange = { address = it },
                        label = "شماره گیرنده پیامک *",
                        placeholder = "09123456789",
                        kind = KashiInputKind.MOBILE,
                        leadingIcon = KashiIcons.Phone
                    )

                    Text(
                        text = "انتخاب سیم‌کارت خروجی:",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    KashiSegmentedControl(
                        items = listOf("سیم ۱ (Slot 0)", "سیم ۲ (Slot 1)"),
                        selectedIndex = slotIndex,
                        onItemSelected = { slotIndex = it },
                        icons = listOf(KashiIcons.SimCard, KashiIcons.SimCard)
                    )

                    OutlinedTextField(
                        value = smsTemplate,
                        onValueChange = { smsTemplate = it },
                        label = { Text("قالب متن پیامک خروجی *", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("درج متغیر:", fontFamily = VazirmatnFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        VariableBadge(text = "درج {{sender}}") { smsTemplate += "{{sender}}" }
                        VariableBadge(text = "درج {{content}}") { smsTemplate += "{{content}}" }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sticky Action Buttons inside Sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isSheetValid) {
                                val jsonMap = jsonFieldsList.filter { it.first.isNotBlank() }.toMap()
                                val dest = RuleDestination(
                                    id = initialDestination?.id ?: UUID.randomUUID().toString(),
                                    enabled = initialDestination?.enabled ?: true,
                                    destinationType = type,
                                    destinationAddress = address.trim(),
                                    outgoingSlotIndex = slotIndex,
                                    smsTemplate = smsTemplate,
                                    jsonFields = if (jsonMap.isNotEmpty()) jsonMap else mapOf("sender" to "{{sender}}", "content" to "{{content}}")
                                )
                                onSaveDestination(dest)
                            }
                        },
                        enabled = isSheetValid,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("تایید و ثبت مقصد", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = closeSheet,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("انصراف", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(number: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontFamily = VazirmatnFontFamily,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = title,
            fontFamily = VazirmatnFontFamily,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VariableBadge(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
