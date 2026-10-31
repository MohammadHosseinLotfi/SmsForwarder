package com.example.smsforwarder

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.ui.MainViewModel
import com.example.smsforwarder.ui.components.MiuiGuideDialog
import com.example.smsforwarder.ui.screens.AddEditRuleScreen
import com.example.smsforwarder.ui.screens.LogsScreen
import com.example.smsforwarder.ui.screens.RulesScreen
import com.example.smsforwarder.ui.theme.SmsForwarderTheme

sealed class AppScreen {
    data object Rules : AppScreen()
    data class AddEditRule(val ruleToEdit: RuleEntity? = null) : AppScreen()
    data object Logs : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (smsGranted) {
            Toast.makeText(this, "مجوزهای دریافت و پردازش پیامک اعطا شد", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "برای انتقال هوشمند پیامک‌ها به مجوز نیاز است", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // درخواست مجوزهای اصلی اندروید
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE
            )
        )

        setContent {
            SmsForwarderTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SmsForwarderApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsForwarderApp(
    viewModel: MainViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Rules) }
    var showMiuiGuide by remember { mutableStateOf(false) }

    if (showMiuiGuide) {
        MiuiGuideDialog(onDismiss = { showMiuiGuide = false })
    }

    Scaffold(
        bottomBar = {
            if (currentScreen !is AppScreen.AddEditRule) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Rules,
                        onClick = { currentScreen = AppScreen.Rules },
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = "قوانین") },
                        label = { Text("قوانین") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Logs,
                        onClick = { currentScreen = AppScreen.Logs },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (deliveries.isNotEmpty()) {
                                        Badge { Text(deliveries.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.History, contentDescription = "گزارشات")
                            }
                        },
                        label = { Text("گزارشات") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Rules -> {
                    RulesScreen(
                        rules = rules,
                        onToggleRule = { viewModel.toggleRuleEnabled(it) },
                        onEditRule = { rule -> currentScreen = AppScreen.AddEditRule(rule) },
                        onDuplicateRule = { viewModel.duplicateRule(it) },
                        onDeleteRule = { viewModel.deleteRule(it) },
                        onAddNewRule = { currentScreen = AppScreen.AddEditRule(null) },
                        onShowMiuiGuide = { showMiuiGuide = true }
                    )
                }
                is AppScreen.AddEditRule -> {
                    AddEditRuleScreen(
                        editingRule = screen.ruleToEdit,
                        onSaveRule = { newRule ->
                            viewModel.saveRule(newRule)
                            currentScreen = AppScreen.Rules
                        },
                        onRunDryRun = { rule, testSender, testContent ->
                            viewModel.runDryRunTest(rule, testSender, testContent)
                        },
                        onCancel = { currentScreen = AppScreen.Rules }
                    )
                }
                is AppScreen.Logs -> {
                    LogsScreen(
                        deliveries = deliveries,
                        onCancelDelivery = { viewModel.cancelDelivery(it) },
                        onClearAllLogs = { viewModel.clearAllLogs() }
                    )
                }
            }
        }
    }
}