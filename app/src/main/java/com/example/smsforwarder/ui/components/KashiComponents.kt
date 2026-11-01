package com.example.smsforwarder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.ui.theme.*
import com.example.smsforwarder.ui.util.*
import kotlinx.coroutines.delay

enum class KashiInputKind {
    TEXT,
    SENDER,
    MOBILE,
    URL
}

/**
 * هدر ثابت برند بالا (KashiBrandTopBar)
 */
@Composable
fun KashiBrandTopBar(
    modifier: Modifier = Modifier,
    isServiceActive: Boolean = true
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(52.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = KashiIcons.ChatSquare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "اسمس فورواردر",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                KashiStatusPill(text = "سرویس فعال", isActive = isServiceActive)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * هدر سیاقی داخل صفحه (KashiPageHeader)
 */
@Composable
fun KashiPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = VazirmatnFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            badgeText?.let { badge ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = badge,
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions
        )
    }
}

/**
 * هدر فشرده و شیک کاشی (KashiTopBar) برای زیرصفحه‌ها
 */
@Composable
fun KashiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    statusPillText: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(52.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(
                            onClick = onNavigationClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = title,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    statusPillText?.let {
                        KashiStatusPill(text = it)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * ۱. کاشی اسپلاش لودر کوتاه (~۹۵۰ میلی‌ثانیه) با انیمیشن رادار عاری از Recomposition
 */
@Composable
fun KashiSplashLoader(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(950)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_splash")

    // موج راداری اول
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    // موج راداری دوم (با اختلاف فاز)
    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 450, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    // تپش نرم هسته مرکزی
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // بخش گرافیکی رادار پیامک
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                // حلقه موج اول (فیروزه‌ای)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            val scale = 1f + (wave1Progress * 0.9f)
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - wave1Progress)
                        }
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.22f))
                        .border(1.5.dp, primaryColor.copy(alpha = 0.65f), CircleShape)
                )

                // حلقه موج دوم (لاجوردی)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            val scale = 1f + (wave2Progress * 0.9f)
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - wave2Progress)
                        }
                        .clip(CircleShape)
                        .background(secondaryColor.copy(alpha = 0.16f))
                        .border(1.5.dp, secondaryColor.copy(alpha = 0.55f), CircleShape)
                )

                // دایره مرکزی آیکون پیامک
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .graphicsLayer {
                            scaleX = coreScale
                            scaleY = coreScale
                        }
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = KashiIcons.ChatSquare,
                        contentDescription = "SMS Forwarder",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // بج کوچک نشانگر انتقال در لبه پایین هسته
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-22).dp, y = (-22).dp)
                        .size(30.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = KashiIcons.Workflow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // نقطه سبز سیگنال فعال در بالا
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 28.dp, y = 28.dp)
                        .size(14.dp),
                    shape = CircleShape,
                    color = KashiSuccessGreen,
                    border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.background)
                ) {}
            }

            Spacer(modifier = Modifier.height(18.dp))

            // عنوان اصلی فارسی
            Text(
                text = "اسمس فورواردر",
                fontFamily = VazirmatnFontFamily,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // تگ انگلیسی نام اپ
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Text(
                    text = "SMS FORWARDER",
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // زیرعنوان
            Text(
                text = "انتقال هوشمند و خودکار پیامک‌ها",
                fontFamily = VazirmatnFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(26.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )
        }
    }
}

/**
 * ۲. کنترل بخش‌بندی شده کاشی (KashiSegmentedControl)
 */
@Composable
fun KashiSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val targetBg = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                val targetContent = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                val animatedBg by animateColorAsState(targetBg, animationSpec = tween(180), label = "seg_bg")
                val animatedContent by animateColorAsState(targetContent, animationSpec = tween(180), label = "seg_content")
                val border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null

                Surface(
                    onClick = { onItemSelected(index) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = animatedBg,
                    border = border,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icons != null && index < icons.size) {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                tint = animatedContent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = title,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedContent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * ۳. ورودی اختصاصی کاشی فارسی (KashiPersianInput)
 */
@Composable
fun KashiPersianInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    kind: KashiInputKind = KashiInputKind.TEXT,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingBadge: SenderBadgeInfo? = null,
    onBadgeClick: (() -> Unit)? = null
) {
    val placeholderPersian = remember(placeholder) { placeholder.toPersianDigits() }

    val autoBadge = remember(value, kind, trailingBadge) {
        if (trailingBadge != null) return@remember trailingBadge
        when (kind) {
            KashiInputKind.SENDER -> {
                if (value.isBlank()) {
                    SenderBadgeInfo("همه فرستنده‌ها", Color(0xFF00897B), Color.White)
                } else {
                    detectSenderBadge(value)
                }
            }
            KashiInputKind.MOBILE -> {
                val operator = detectMobileOperator(value)
                operator?.let { SenderBadgeInfo(it, Color(0xFF4F46E5), Color.White) }
            }
            KashiInputKind.URL -> {
                val lower = value.toEnglishDigits().lowercase().trim()
                if (lower.startsWith("https://")) {
                    SenderBadgeInfo("HTTPS ایمن", Color(0xFF10B981), Color.White)
                } else if (lower.startsWith("http://")) {
                    SenderBadgeInfo("HTTP ناایمن", Color(0xFFF59E0B), Color.Black)
                } else null
            }
            else -> null
        }
    }

    val liveError = remember(value, kind, isError, errorMessage) {
        if (isError) return@remember errorMessage
        when (kind) {
            KashiInputKind.MOBILE -> {
                val eng = value.toEnglishDigits().trim()
                if (eng.isNotEmpty() && !eng.startsWith("09") && !eng.startsWith("+989") && !eng.startsWith("989")) {
                    "فرمت شماره موبایل معتبر نیست — با ۰۹ شروع شود"
                } else null
            }
            KashiInputKind.URL -> {
                val eng = value.toEnglishDigits().lowercase().trim()
                if (eng.isNotEmpty() && !eng.startsWith("http://") && !eng.startsWith("https://")) {
                    "آدرس وب‌هوک باید با http یا https شروع شود"
                } else null
            }
            else -> null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = VazirmatnFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            autoBadge?.let { badge ->
                Surface(
                    color = badge.backgroundColor,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(enabled = onBadgeClick != null) { onBadgeClick?.invoke() }
                ) {
                    Text(
                        text = badge.label,
                        fontFamily = VazirmatnFontFamily,
                        color = badge.textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                val processed = when (kind) {
                    KashiInputKind.SENDER -> input.fixPersianText()
                    KashiInputKind.MOBILE -> input.fixPersianText()
                    KashiInputKind.URL -> input.toEnglishDigits().fixPersianText()
                    else -> input.fixPersianText()
                }
                onValueChange(processed)
            },
            placeholder = { Text(placeholderPersian, fontFamily = VazirmatnFontFamily, fontSize = 14.sp) },
            singleLine = singleLine,
            minLines = minLines,
            isError = liveError != null,
            keyboardOptions = when (kind) {
                KashiInputKind.MOBILE -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                KashiInputKind.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri)
                else -> KeyboardOptions.Default
            },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (liveError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = liveError,
                fontFamily = VazirmatnFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * ۴. برگ تقویم کاشی (KashiDateTile)
 */
@Composable
fun KashiDateTile(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val jalali = remember(timestamp) { timestampToJalali(timestamp) }

    Surface(
        modifier = modifier.width(56.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = jalali.monthName,
                    fontFamily = VazirmatnFontFamily,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = jalali.day.toPersianDigits(),
                fontFamily = VazirmatnFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
            )

            Text(
                text = jalali.formattedTimeShort,
                fontFamily = VazirmatnFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/**
 * ۵. حالت خالی کاشی (KashiEmptyState)
 */
@Composable
fun KashiEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                fontFamily = VazirmatnFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontFamily = VazirmatnFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(KashiIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * دکمه وضعیت پلس‌دار کاشی (KashiStatusPill)
 */
@Composable
fun KashiStatusPill(
    text: String = "سرویس فعال",
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        val s = if (isActive) alphaAnim else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(if (isActive) KashiSuccessGreen else Color.Gray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontFamily = VazirmatnFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * بنر کاشی (KashiBanner)
 */
@Composable
fun KashiBanner(
    title: String,
    description: String,
    icon: ImageVector,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(actionText, fontFamily = VazirmatnFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * نمودار جریان بصری داخل کارت قانون (KashiFlowDiagram)
 */
@Composable
fun KashiFlowDiagram(
    senderText: String,
    destinationText: String,
    destinationTypeLabel: String,
    isUrl: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("فرستنده", fontFamily = VazirmatnFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = senderText,
                    fontFamily = VazirmatnFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = KashiIcons.ArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.End
            ) {
                Text("مقصد ($destinationTypeLabel)", fontFamily = VazirmatnFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isUrl) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = destinationText,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = destinationText,
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * دیالوگ انتخاب تاریخ جلالی کاشی (KashiJalaliDatePickerDialog)
 */
@Composable
fun KashiJalaliDatePickerDialog(
    initialDate: Triple<Int, Int, Int>?,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val todayJalali = remember { timestampToJalali(System.currentTimeMillis()) }

    var currentYear by remember { mutableIntStateOf(initialDate?.first ?: todayJalali.year) }
    var currentMonth by remember { mutableIntStateOf(initialDate?.second ?: todayJalali.month) }
    var selectedDay by remember { mutableIntStateOf(initialDate?.third ?: todayJalali.day) }

    val daysInMonth = remember(currentYear, currentMonth) { jalaliDaysInMonth(currentYear, currentMonth) }
    val firstDayOffset = remember(currentYear, currentMonth) { getJalaliFirstDayOfWeek(currentYear, currentMonth) }

    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    val chunkedWeeks = remember(daysInMonth, firstDayOffset) {
        val days = (1..daysInMonth).toList()
        val itemsList = mutableListOf<Int?>()
        repeat(firstDayOffset) { itemsList.add(null) }
        days.forEach { itemsList.add(it) }
        itemsList.chunked(7)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = KashiIcons.Calendar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "انتخاب تاریخ شمسی",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = KashiIcons.XCircle,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentMonth > 1) {
                                    currentMonth--
                                } else {
                                    currentMonth = 12
                                    currentYear--
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = KashiIcons.ChevronRight,
                                contentDescription = "ماه قبل",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${PERSIAN_MONTH_NAMES[currentMonth - 1]} ${currentYear.toPersianDigits()}",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                if (currentMonth < 12) {
                                    currentMonth++
                                } else {
                                    currentMonth = 1
                                    currentYear++
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = KashiIcons.ChevronLeft,
                                contentDescription = "ماه بعد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    chunkedWeeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayOrNull ->
                                if (dayOrNull == null) {
                                    Spacer(modifier = Modifier.size(36.dp))
                                } else {
                                    val day = dayOrNull
                                    val isSelected = day == selectedDay
                                    val isToday = currentYear == todayJalali.year && currentMonth == todayJalali.month && day == todayJalali.day

                                    Surface(
                                        onClick = { selectedDay = day },
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        },
                                        border = if (isToday && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = day.toPersianDigits(),
                                                fontFamily = VazirmatnFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            repeat(7 - week.size) {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onDateSelected(currentYear, currentMonth, selectedDay)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("تایید تاریخ", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = null
    )
}
