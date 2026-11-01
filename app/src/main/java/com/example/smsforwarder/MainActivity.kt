package com.example.smsforwarder

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.ui.MainViewModel
import com.example.smsforwarder.ui.components.KashiBrandTopBar
import com.example.smsforwarder.ui.components.KashiIcons
import com.example.smsforwarder.ui.components.KashiSplashLoader
import com.example.smsforwarder.ui.components.MiuiGuideDialog
import com.example.smsforwarder.ui.screens.AddEditRuleScreen
import com.example.smsforwarder.ui.screens.DashboardScreen
import com.example.smsforwarder.ui.screens.LogFilter
import com.example.smsforwarder.ui.screens.LogsScreen
import com.example.smsforwarder.ui.screens.RulesScreen
import com.example.smsforwarder.ui.theme.SmsForwarderTheme
import com.example.smsforwarder.ui.theme.VazirmatnFontFamily
import com.example.smsforwarder.ui.util.toPersianDigits
import kotlinx.coroutines.launch

sealed class AppScreen {
    data object Dashboard : AppScreen()
    data object Rules : AppScreen()
    data class AddEditRule(val ruleToEdit: RuleEntity? = null) : AppScreen()
    data class Logs(
        val initialFilter: LogFilter = LogFilter.ALL,
        val filterToday: Boolean = false
    ) : AppScreen()
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

@Composable
fun SmsForwarderApp(
    viewModel: MainViewModel = viewModel()
) {
    var isSplashFinished by remember { mutableStateOf(false) }

    Crossfade(
        targetState = isSplashFinished,
        animationSpec = tween(durationMillis = 400),
        label = "splash_transition"
    ) { splashDone ->
        if (!splashDone) {
            KashiSplashLoader(
                onFinished = { isSplashFinished = true }
            )
        } else {
            MainAppContent(viewModel = viewModel)
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var showMiuiGuide by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var logsInitialFilter by remember { mutableStateOf(LogFilter.ALL) }
    var logsFilterToday by remember { mutableStateOf(false) }

    val isAddEditMode = currentScreen is AppScreen.AddEditRule

    BackHandler(enabled = isAddEditMode || pagerState.currentPage != 0) {
        if (isAddEditMode) {
            currentScreen = AppScreen.Rules
        } else {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    if (showMiuiGuide) {
        MiuiGuideDialog(onDismiss = { showMiuiGuide = false })
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isAddEditMode) {
                KashiBrandTopBar()
            }
        },
        bottomBar = {
            if (!isAddEditMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .height(46.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dashboard Tab Pill (Page 0)
                        val isDashboardSelected = pagerState.currentPage == 0
                        val dashboardBg by animateColorAsState(
                            if (isDashboardSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            animationSpec = tween(180),
                            label = "dashboard_bg"
                        )
                        val dashboardContent by animateColorAsState(
                            if (isDashboardSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "dashboard_content"
                        )

                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = dashboardBg,
                            border = if (isDashboardSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Dashboard,
                                    contentDescription = null,
                                    tint = dashboardContent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "داشبورد",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isDashboardSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = dashboardContent
                                )
                            }
                        }

                        // Rules Tab Pill (Page 1)
                        val isRulesSelected = pagerState.currentPage == 1
                        val rulesBg by animateColorAsState(
                            if (isRulesSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            animationSpec = tween(180),
                            label = "rules_bg"
                        )
                        val rulesContent by animateColorAsState(
                            if (isRulesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "rules_content"
                        )

                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = rulesBg,
                            border = if (isRulesSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Workflow,
                                    contentDescription = null,
                                    tint = rulesContent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "قوانین انتقال",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isRulesSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = rulesContent
                                )
                            }
                        }

                        // Logs Tab Pill (Page 2)
                        val isLogsSelected = pagerState.currentPage == 2
                        val logsBg by animateColorAsState(
                            if (isLogsSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            animationSpec = tween(180),
                            label = "logs_bg"
                        )
                        val logsContent by animateColorAsState(
                            if (isLogsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "logs_content"
                        )

                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = logsBg,
                            border = if (isLogsSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = KashiIcons.Receipt,
                                    contentDescription = null,
                                    tint = logsContent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "گزارشات ارسال",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isLogsSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = logsContent
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isAddEditMode) {
                val ruleToEdit = (currentScreen as? AppScreen.AddEditRule)?.ruleToEdit
                AddEditRuleScreen(
                    editingRule = ruleToEdit,
                    onSaveRule = { newRule ->
                        viewModel.saveRule(newRule)
                        currentScreen = AppScreen.Rules
                    },
                    onRunDryRun = { rule, testSender, testContent ->
                        viewModel.runDryRunTest(rule, testSender, testContent)
                    },
                    onCancel = { currentScreen = AppScreen.Rules }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> DashboardScreen(
                            rules = rules,
                            deliveries = deliveries,
                            onToggleRule = { viewModel.toggleRuleEnabled(it) },
                            onEditRule = { rule -> currentScreen = AppScreen.AddEditRule(rule) },
                            onNavigateToLogsWithFilter = { filter ->
                                logsInitialFilter = filter
                                logsFilterToday = true
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            },
                            onShowMiuiGuide = { showMiuiGuide = true }
                        )
                        1 -> RulesScreen(
                            rules = rules,
                            onToggleRule = { viewModel.toggleRuleEnabled(it) },
                            onEditRule = { rule -> currentScreen = AppScreen.AddEditRule(rule) },
                            onDuplicateRule = { viewModel.duplicateRule(it) },
                            onDeleteRule = { viewModel.deleteRule(it) },
                            onAddNewRule = { currentScreen = AppScreen.AddEditRule(null) },
                            onShowMiuiGuide = { showMiuiGuide = true }
                        )
                        2 -> LogsScreen(
                            deliveries = deliveries,
                            rules = rules,
                            initialFilter = logsInitialFilter,
                            initialFilterToday = logsFilterToday,
                            onFilterConsumed = {
                                logsFilterToday = false
                            },
                            onCancelDelivery = { viewModel.cancelDelivery(it) },
                            onResendDelivery = { viewModel.resendDelivery(it) },
                            onClearAllLogs = { viewModel.clearAllLogs() }
                        )
                    }
                }
            }
        }
    }
}
