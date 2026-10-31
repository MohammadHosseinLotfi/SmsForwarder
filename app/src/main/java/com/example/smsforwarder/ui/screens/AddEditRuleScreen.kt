package com.example.smsforwarder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.ui.DryRunResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleScreen(
    editingRule: RuleEntity? = null,
    onSaveRule: (RuleEntity) -> Unit,
    onRunDryRun: (RuleEntity, String, String) -> DryRunResult,
    onCancel: () -> Unit
) {
    val gson = remember { Gson() }

    var name by remember { mutableStateOf(editingRule?.name ?: "") }
    var sender by remember { mutableStateOf(editingRule?.sender ?: "") }
    var containsText by remember { mutableStateOf(editingRule?.containsText ?: "") }
    var destinationType by remember { mutableStateOf(editingRule?.destinationType ?: DestinationType.URL) }
    var destinationAddress by remember { mutableStateOf(editingRule?.destinationAddress ?: "") }
    var outgoingSlotIndex by remember { mutableIntStateOf(editingRule?.outgoingSlotIndex ?: 0) }
    var smsTemplate by remember { mutableStateOf(editingRule?.smsTemplate ?: "{{content}}") }

    // پارس اولیه JSON fields
    val initialJsonFields = remember(editingRule) {
        if (editingRule != null) {
            val mapType = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
            try {
                gson.fromJson<Map<String, String>>(editingRule.jsonFieldsJson, mapType) ?: mapOf("form" to "{{sender}}", "content" to "{{content}}")
            } catch (_: Exception) {
                mapOf("form" to "{{sender}}", "content" to "{{content}}")
            }
        } else {
            mapOf("form" to "{{sender}}", "content" to "{{content}}")
        }
    }

    var jsonFieldsList by remember {
        mutableStateOf(initialJsonFields.map { Pair(it.key, it.value) })
    }

    // مقادیر ورودی برای بخش تست خشک (Dry Run)
    var testSender by remember { mutableStateOf(sender.ifBlank { "09999987641" }) }
    var testContent by remember { mutableStateOf("واریز پول\nمبلغ: ۱,۰۰۰,۰۰۰ ریال به حساب نشست") }
    var dryRunResult by remember { mutableStateOf<DryRunResult?>(null) }

    val scrollState = rememberScrollState()

    // ساخت RuleEntity موقت برای تست خشک یا ذخیره
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingRule == null) "افزودن قانون جدید" else "ویرایش قانون", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
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
            // ۱. مشخصات عمومی قانون
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام قانون *") },
                placeholder = { Text("مانند: انتقال واریزهای بلو به سرور") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("فرستنده پیامک (شماره یا شناسه متنی) *") },
                placeholder = { Text("مانند: 09999987641 یا B.Pasargad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            OutlinedTextField(
                value = containsText,
                onValueChange = { containsText = it },
                label = { Text("عبارت داخل متن (اختیاری)") },
                placeholder = { Text("مانند: واریز پول یا + (خالی یعنی تمام پیامک‌های این فرستنده)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
            )

            HorizontalDivider()

            // ۲. انتخاب نوع مقصد (URL یا SMS)
            Text("نوع مقصد ارسال:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = destinationType == DestinationType.URL,
                    onClick = { destinationType = DestinationType.URL },
                    label = { Text("وب‌هوک اینترنتی (URL / JSON)") },
                    leadingIcon = { Icon(Icons.Default.Http, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = destinationType == DestinationType.SMS,
                    onClick = { destinationType = DestinationType.SMS },
                    label = { Text("پیامک (SMS)") },
                    leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (destinationType == DestinationType.URL) {
                OutlinedTextField(
                    value = destinationAddress,
                    onValueChange = { destinationAddress = it },
                    label = { Text("آدرس URL مقصد (HTTPS) *") },
                    placeholder = { Text("https://example.com/api/sms") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )

                Text("فیلدهای JSON سفارشی:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                            label = { Text("کلید") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { newVal ->
                                val updated = jsonFieldsList.toMutableList()
                                updated[index] = Pair(pair.first, newVal)
                                jsonFieldsList = updated
                            },
                            label = { Text("مقدار / متغیر") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (jsonFieldsList.size > 1) {
                                    jsonFieldsList = jsonFieldsList.filterIndexed { i, _ -> i != index }
                                }
                            }
                        ) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = "حذف کلید", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            jsonFieldsList = jsonFieldsList + Pair("", "{{content}}")
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("افزودن فیلد جدید")
                    }

                    AssistChip(
                        onClick = {
                            if (jsonFieldsList.isNotEmpty()) {
                                val last = jsonFieldsList.last()
                                val updated = jsonFieldsList.toMutableList()
                                updated[updated.lastIndex] = Pair(last.first, "${last.second}{{sender}}")
                                jsonFieldsList = updated
                            }
                        },
                        label = { Text("+ {{sender}}") }
                    )
                    AssistChip(
                        onClick = {
                            if (jsonFieldsList.isNotEmpty()) {
                                val last = jsonFieldsList.last()
                                val updated = jsonFieldsList.toMutableList()
                                updated[updated.lastIndex] = Pair(last.first, "${last.second}{{content}}")
                                jsonFieldsList = updated
                            }
                        },
                        label = { Text("+ {{content}}") }
                    )
                }
            } else {
                OutlinedTextField(
                    value = destinationAddress,
                    onValueChange = { destinationAddress = it },
                    label = { Text("شماره گیرنده پیامک *") },
                    placeholder = { Text("09123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )

                Text("انتخاب سیم‌کارت خروجی:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = outgoingSlotIndex == 0,
                        onClick = { outgoingSlotIndex = 0 },
                        label = { Text("سیم ۱ (Slot 0)") },
                        leadingIcon = { Icon(Icons.Default.SimCard, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = outgoingSlotIndex == 1,
                        onClick = { outgoingSlotIndex = 1 },
                        label = { Text("سیم ۲ (Slot 1)") },
                        leadingIcon = { Icon(Icons.Default.SimCard, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = smsTemplate,
                    onValueChange = { smsTemplate = it },
                    label = { Text("قالب متن پیامک خروجی *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { smsTemplate += "{{sender}}" },
                        label = { Text("درج {{sender}}") }
                    )
                    AssistChip(
                        onClick = { smsTemplate += "{{content}}" },
                        label = { Text("درج {{content}}") }
                    )
                }
            }

            HorizontalDivider()

            // ۳. کارت تست خشک (Dry-Run Test) طبق سند
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بخش تست خشک قانون (Dry-Run)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = "بدون انجام هیچ درخواست واقعی، می‌توانید صحت کارکرد قانون را بیازمایید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = testSender,
                        onValueChange = { testSender = it },
                        label = { Text("فرستنده آزمایشی") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testContent,
                        onValueChange = { testContent = it },
                        label = { Text("متن پیامک آزمایشی") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val entity = buildCurrentEntity()
                            dryRunResult = onRunDryRun(entity, testSender, testContent)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اجرای تست خشک")
                    }

                    dryRunResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.isMatched) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (result.isMatched) "✔ تطبیق کامل برقرار است" else "✖ عدم تطبیق",
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.isMatched) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(
                                    text = result.matchReason,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                                if (result.isMatched) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("پیش‌نمایش خروجی نهایی:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = result.renderedOutput,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // دکمه ذخیره نهایی
            val isFormValid = name.isNotBlank() && sender.isNotBlank() && destinationAddress.isNotBlank()
            Button(
                onClick = {
                    if (isFormValid) {
                        onSaveRule(buildCurrentEntity())
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ذخیره قانون", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}