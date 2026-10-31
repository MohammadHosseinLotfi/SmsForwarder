package com.example.smsforwarder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.ui.components.*
import com.example.smsforwarder.ui.theme.VazirmatnFontFamily
import com.example.smsforwarder.ui.util.*

@Composable
fun RulesScreen(
    rules: List<RuleEntity>,
    onToggleRule: (RuleEntity) -> Unit,
    onEditRule: (RuleEntity) -> Unit,
    onDuplicateRule: (RuleEntity) -> Unit,
    onDeleteRule: (RuleEntity) -> Unit,
    onAddNewRule: () -> Unit,
    onShowMiuiGuide: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Surface(
                onClick = onAddNewRule,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier.height(42.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(KashiIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("قانون جدید", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            KashiPageHeader(
                title = "مدیریت قوانین",
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (rules.isEmpty()) {
                KashiEmptyState(
                    icon = KashiIcons.Workflow,
                    title = "هیچ قانونی تعریف نشده است",
                    description = "برای شروع انتقال هوشمند پیامک‌ها، دکمه «قانون جدید» را بزنید.",
                    actionLabel = "قانون جدید",
                    onAction = onAddNewRule
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        Box(modifier = Modifier.animateItem()) {
                            RuleCard(
                                rule = rule,
                                onToggle = { onToggleRule(rule) },
                                onEdit = { onEditRule(rule) },
                                onDuplicate = { onDuplicateRule(rule) },
                                onDelete = { onDeleteRule(rule) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleCard(
    rule: RuleEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "حذف قانون",
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف قانون «${rule.name}» اطمینان دارید؟",
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
                            showDeleteConfirm = false
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("حذف قانون", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = false },
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (rule.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (rule.enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = if (rule.enabled) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rule.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
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
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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
                    modifier = Modifier.scale(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            KashiFlowDiagram(
                senderText = if (isAllSenders) "همه فرستنده‌ها" else rule.sender.toPersianDigits(),
                destinationText = rule.destinationAddress.toPersianDigits(),
                destinationTypeLabel = if (rule.destinationType == DestinationType.URL) "وب‌هوک URL" else "پیامک SMS"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!rule.containsText.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = KashiIcons.Filter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "فیلتر: «${rule.containsText.toPersianDigits()}»",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                if (rule.destinationType == DestinationType.SMS) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = KashiIcons.SimCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "سیم‌کارت ${(rule.outgoingSlotIndex + 1).toPersianDigits()}",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = KashiIcons.Edit,
                        contentDescription = "ویرایش",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ویرایش", fontFamily = VazirmatnFontFamily, fontSize = 13.sp)
                }

                TextButton(
                    onClick = onDuplicate,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = KashiIcons.Copy,
                        contentDescription = "کپی",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کپی", fontFamily = VazirmatnFontFamily, fontSize = 13.sp)
                }

                TextButton(
                    onClick = { showDeleteConfirm = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = KashiIcons.Trash,
                        contentDescription = "حذف",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", fontFamily = VazirmatnFontFamily, fontSize = 13.sp)
                }
            }
        }
    }
}
