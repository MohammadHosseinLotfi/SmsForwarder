package com.example.smsforwarder.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.ui.DryRunResult
import com.example.smsforwarder.ui.components.*
import com.example.smsforwarder.ui.theme.*
import com.example.smsforwarder.ui.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun AddEditRuleScreen(
    editingRule: RuleEntity? = null,
    onSaveRule: (RuleEntity) -> Unit,
    onRunDryRun: (RuleEntity, String, String) -> DryRunResult,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val gson = remember { Gson() }

    var name by remember { mutableStateOf(editingRule?.name ?: "") }
    var sender by remember { mutableStateOf(editingRule?.sender ?: "") }
    var containsText by remember { mutableStateOf(editingRule?.containsText ?: "") }
    var destinationType by remember { mutableStateOf(editingRule?.destinationType ?: DestinationType.URL) }
    var destinationAddress by remember { mutableStateOf(editingRule?.destinationAddress ?: "") }
    var outgoingSlotIndex by remember { mutableIntStateOf(editingRule?.outgoingSlotIndex ?: 0) }
    var smsTemplate by remember { mutableStateOf(editingRule?.smsTemplate ?: "{{content}}") }

    val initialJsonFields = remember(editingRule) {
        if (editingRule != null) {
            val mapType = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
            try {
                gson.fromJson<Map<String, String>>(editingRule.jsonFieldsJson, mapType) ?: mapOf("sender" to "{{sender}}", "content" to "{{content}}")
            } catch (_: Exception) {
                mapOf("sender" to "{{sender}}", "content" to "{{content}}")
            }
        } else {
            mapOf("sender" to "{{sender}}", "content" to "{{content}}")
        }
    }

    var jsonFieldsList by remember {
        mutableStateOf(initialJsonFields.map { Pair(it.key, it.value) })
    }

    var testSender by remember { mutableStateOf(sender.ifBlank { "09999987641" }) }
    var testContent by remember { mutableStateOf("واریز پول\nمبلغ: ۱,۰۰۰,۰۰۰ ریال به حساب نشست") }
    var dryRunResult by remember { mutableStateOf<DryRunResult?>(null) }

    val scrollState = rememberScrollState()

    fun buildCurrentEntity(): RuleEntity {
        val jsonMap = jsonFieldsList
            .filter { it.first.isNotBlank() }
            .toMap()
        return RuleEntity(
            id = editingRule?.id ?: 0,
            name = name.trim(),
            enabled = editingRule?.enabled ?: true,
            sender = sender.trim(),
            containsText = containsText.trim().ifBlank { null },
            destinationType = destinationType,
            destinationAddress = destinationAddress.trim(),
            outgoingSlotIndex = outgoingSlotIndex,
            smsTemplate = smsTemplate,
            jsonFieldsJson = gson.toJson(jsonMap)
        )
    }

    val isFormValid = name.isNotBlank() && destinationAddress.isNotBlank()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            // ==================== بخش ۰۲: مقصد و قالب ارسال ====================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(number = "۰۲", title = "مقصد و قالب ارسال")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "نوع مقصد ارسال:",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        KashiSegmentedControl(
                            items = listOf("وب‌هوک اینترنتی (URL)", "پیامک (SMS)"),
                            selectedIndex = if (destinationType == DestinationType.URL) 0 else 1,
                            onItemSelected = { index ->
                                destinationType = if (index == 0) DestinationType.URL else DestinationType.SMS
                            },
                            icons = listOf(KashiIcons.Webhook, KashiIcons.ChatSquare)
                        )

                        if (destinationType == DestinationType.URL) {
                            KashiPersianInput(
                                value = destinationAddress,
                                onValueChange = { destinationAddress = it },
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

                            jsonFieldsList.forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pair.first,
                                        onValueChange = { newKey ->
                                            val updated = jsonFieldsList.toMutableList()
                                            updated[index] = Pair(newKey, pair.second)
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
                                            updated[index] = Pair(pair.first, newVal)
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
                                                jsonFieldsList = jsonFieldsList.filterIndexed { i, _ -> i != index }
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
                                Text("درج متغیر در آخرین فیلد:", fontFamily = VazirmatnFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                value = destinationAddress,
                                onValueChange = { destinationAddress = it },
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
                                selectedIndex = outgoingSlotIndex,
                                onItemSelected = { outgoingSlotIndex = it },
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
                    }
                }
            }

            // ==================== بخش ۰۳: آزمایش زنده (Dry-Run) ====================
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(number = "۰۳", title = "آزمایش زنده (Dry-Run)")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = KashiSoftTurquoise.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Flask,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "آزمایشگاه سنجش قانون (Dry-Run)",
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "بدون ارسال هیچ درخواست واقعی شبکه یا SMS",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        KashiPersianInput(
                            value = testSender,
                            onValueChange = { testSender = it },
                            label = "فرستنده آزمایشی",
                            placeholder = "09999987641",
                            kind = KashiInputKind.SENDER
                        )

                        KashiPersianInput(
                            value = testContent,
                            onValueChange = { testContent = it },
                            label = "متن پیامک آزمایشی",
                            placeholder = "واریز پول...",
                            kind = KashiInputKind.TEXT,
                            singleLine = false,
                            minLines = 2
                        )

                        OutlinedButton(
                            onClick = {
                                val entity = buildCurrentEntity()
                                dryRunResult = onRunDryRun(entity, testSender, testContent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(KashiIcons.Flask, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اجرای تست خشک", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        dryRunResult?.let { result ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (result.isMatched) KashiSuccessContainer else KashiErrorContainer,
                                border = BorderStroke(1.dp, if (result.isMatched) KashiSuccessGreen else KashiErrorRose),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (result.isMatched) KashiIcons.Success else KashiIcons.XCircle,
                                            contentDescription = null,
                                            tint = if (result.isMatched) KashiSuccessGreen else KashiErrorRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (result.isMatched) "✔ تطبیق کامل برقرار است" else "✖ عدم تطبیق",
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (result.isMatched) Color(0xFF065F46) else Color(0xFF991B1B)
                                        )
                                    }

                                    Text(
                                        text = result.matchReason,
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                    )

                                    if (result.isMatched) {
                                        Text("پیش‌نمایش خروجی نهایی:", fontFamily = VazirmatnFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(KashiDeepSlate)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = result.renderedOutput,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = KashiSkyTurquoise
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
