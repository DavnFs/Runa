package id.rona.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.R
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.domain.model.Confidence
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.components.RonaEmptyState
import id.rona.app.ui.components.RonaErrorState
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.components.RonaTopBar
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaMotion
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSuccess = uiState is HomeUiState.Success
    val todayFormatted = if (isSuccess) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID")))
    } else {
        null
    }

    var showEditPeriodSheet by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var editingPeriodId by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var initialDateForPeriod by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        RonaTopBar(
            subtitle = todayFormatted,
            onOpenSettings = onOpenSettings,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> RonaLoadingSkeleton(message = "Menyiapkan halamanmu…")
                is HomeUiState.Empty -> HomeEmptyContent(
                    onStartPeriod = {
                        editingPeriodId = null
                        initialDateForPeriod = LocalDate.now()
                        showEditPeriodSheet = true
                    },
                )
                is HomeUiState.Error -> RonaErrorState(
                    message = state.userMessage,
                    onRetry = viewModel::retry,
                )
                is HomeUiState.Success -> HomeSuccessContent(
                    homeData = state.homeData,
                    onStartPeriod = viewModel::startPeriod,
                    onEndPeriod = viewModel::endPeriod,
                    onEditPeriod = { id, date ->
                        editingPeriodId = id
                        initialDateForPeriod = date
                        showEditPeriodSheet = true
                    },
                    onLogToday = onLogToday,
                    onOpenCalendar = onOpenCalendar,
                    onOpenInsights = onOpenInsights,
                )
            }
        }
    }

    if (showEditPeriodSheet) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showEditPeriodSheet = false },
            sheetState = sheetState,
            shape = id.rona.app.ui.theme.RonaBottomSheetShape,
        ) {
            id.rona.app.ui.period.PeriodEditBottomSheet(
                periodId = editingPeriodId,
                initialDate = initialDateForPeriod,
                onDismiss = { showEditPeriodSheet = false },
                onSaved = { showEditPeriodSheet = false },
                onDeleted = { showEditPeriodSheet = false },
            )
        }
    }
}

/**
 * Pixel-accurate abstract concentric hero circle from reference PDF Page 1.
 * - Solid terracotta rose outer ring (#8D4355, 13dp stroke)
 * - Inner soft blush circle fill (#FAF2F4)
 * - Middle dashed concentric ring (#C9BAC0, 1.5dp stroke, 10px dash/gap)
 * - Center botanical 3-petal seed motif in solid rose (#8D4355)
 */
@Composable
fun RonaEmptyHeroCircle(modifier: Modifier = Modifier) {
    val colors = LocalRonaColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(236.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val strokeWidthPx = 13.dp.toPx()
            val outerRadius = (size.minDimension / 2f) - (strokeWidthPx / 2f)

            // Inner soft blush fill
            drawCircle(
                color = colors.surfaceSoft,
                radius = outerRadius,
                center = centerOffset,
            )

            // Outer solid terracotta ring
            drawCircle(
                color = colors.cyclePrimary,
                radius = outerRadius,
                center = centerOffset,
                style = Stroke(width = strokeWidthPx),
            )

            // Inner dashed concentric ring
            val dashedRadius = outerRadius * 0.64f
            drawCircle(
                color = colors.inkTertiary.copy(alpha = 0.45f),
                radius = dashedRadius,
                center = centerOffset,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                ),
            )

            // Center abstract 3-petal seed motif
            val petalColor = colors.cyclePrimary
            val cx = centerOffset.x
            val cy = centerOffset.y

            // Top vertical petal
            val topPetal = Path().apply {
                moveTo(cx, cy - 24.dp.toPx())
                cubicTo(
                    cx + 7.dp.toPx(), cy - 17.dp.toPx(),
                    cx + 7.dp.toPx(), cy - 7.dp.toPx(),
                    cx, cy - 2.dp.toPx(),
                )
                cubicTo(
                    cx - 7.dp.toPx(), cy - 7.dp.toPx(),
                    cx - 7.dp.toPx(), cy - 17.dp.toPx(),
                    cx, cy - 24.dp.toPx(),
                )
                close()
            }
            drawPath(topPetal, color = petalColor)

            // Bottom-left petal
            val leftPetal = Path().apply {
                moveTo(cx - 2.dp.toPx(), cy + 2.dp.toPx())
                cubicTo(
                    cx - 6.dp.toPx(), cy - 2.dp.toPx(),
                    cx - 15.dp.toPx(), cy - 4.dp.toPx(),
                    cx - 20.dp.toPx(), cy + 4.dp.toPx(),
                )
                cubicTo(
                    cx - 14.dp.toPx(), cy + 12.dp.toPx(),
                    cx - 6.dp.toPx(), cy + 10.dp.toPx(),
                    cx - 2.dp.toPx(), cy + 2.dp.toPx(),
                )
                close()
            }
            drawPath(leftPetal, color = petalColor)

            // Bottom-right petal
            val rightPetal = Path().apply {
                moveTo(cx + 2.dp.toPx(), cy + 2.dp.toPx())
                cubicTo(
                    cx + 6.dp.toPx(), cy - 2.dp.toPx(),
                    cx + 15.dp.toPx(), cy - 4.dp.toPx(),
                    cx + 20.dp.toPx(), cy + 4.dp.toPx(),
                )
                cubicTo(
                    cx + 14.dp.toPx(), cy + 12.dp.toPx(),
                    cx + 6.dp.toPx(), cy + 10.dp.toPx(),
                    cx + 2.dp.toPx(), cy + 2.dp.toPx(),
                )
                close()
            }
            drawPath(rightPetal, color = petalColor)
        }
    }
}

/**
 * Custom full-pill button matching the reference PDF frame.
 */
@Composable
fun RunaPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalRonaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = if (isPressed) 0.97f else 1f

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RonaPillShape,
        color = colors.cyclePrimary,
        contentColor = Color.White,
        modifier = modifier
            .height(54.dp)
            .scale(pressScale),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    color = Color.White,
                ),
            )
        }
    }
}

@Composable
fun HomeEmptyContent(
    onStartPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RonaEmptyHeroCircle()

        Spacer(Modifier.height(12.dp))

        // Headline
        Text(
            text = "Mulai mengenali pola\ntubuhmu",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 35.sp,
                letterSpacing = (-0.3).sp,
            ),
            color = colors.inkPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        // Supporting description
        Text(
            text = "Catat periode terakhir untuk membuat\nperkiraan yang lebih personal.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.inkSecondary,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )

        Spacer(Modifier.height(32.dp))

        // Primary action pill button
        RunaPrimaryPillButton(
            text = "+ Catat periode terakhir",
            onClick = onStartPeriod,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 320.dp),
        )

        Spacer(Modifier.height(24.dp))

        // Privacy reassurance subtext
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.inkTertiary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Data disimpan dan dienkripsi di perangkat ini.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = colors.inkTertiary,
                ),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeSuccessContent(
    homeData: HomeData,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit,
    onEditPeriod: (Long?, LocalDate) -> Unit,
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val colors = LocalRonaColors.current

    // 1. Primary Action Button: Pill button matching Figma
    RunaPrimaryPillButton(
        text = "Catat keadaanmu hari ini",
        onClick = onLogToday,
        modifier = Modifier.fillMaxWidth(),
    )

    // 2. Hero Cycle Arc Card (Clickable to edit period)
    RonaCycleHero(
        homeData = homeData,
        onEditPeriod = {
            homeData.latestPeriod?.let {
                onEditPeriod(it.id, it.startDate)
            } ?: onEditPeriod(null, LocalDate.now())
        },
    )

    // 3. One maturity-appropriate, data-backed insight.
    homeData.primaryInsight.primaryCard?.let { insight ->
        RonaHomeInsightCard(
            title = insight.title,
            text = insight.explanation ?: "Pola pada catatanmu, bukan diagnosis.",
            onClick = onOpenInsights,
        )
    }

    // 4. Quick Action Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (homeData.isPeriodActive) {
            id.rona.app.ui.components.RonaSecondaryButton(
                text = "Selesaikan periode",
                onClick = onEndPeriod,
                modifier = Modifier.weight(1f),
            )
        } else {
            id.rona.app.ui.components.RonaSecondaryButton(
                text = "Mulai periode",
                onClick = onStartPeriod,
                modifier = Modifier.weight(1f),
            )
        }
        if (homeData.latestPeriod != null) {
            id.rona.app.ui.components.RonaSecondaryButton(
                text = "Edit periode",
                onClick = { onEditPeriod(homeData.latestPeriod.id, homeData.latestPeriod.startDate) },
                modifier = Modifier.weight(1f),
            )
        } else {
            id.rona.app.ui.components.RonaSecondaryButton(
                text = "Buka kalender",
                onClick = onOpenCalendar,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // 5. Privacy subtext
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = colors.inkTertiary,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Data disimpan dan dienkripsi di perangkat ini.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = colors.inkTertiary,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Pixel-accurate hero cycle display matching Figma "Beranda (Aktif)".
 */
@Composable
fun RonaCycleHero(
    homeData: HomeData,
    modifier: Modifier = Modifier,
    onEditPeriod: (() -> Unit)? = null,
) {
    val colors = LocalRonaColors.current
    val cycleDay = homeData.cycleDay
    val dayInCycle = (cycleDay ?: 1).coerceIn(1, 35)
    val progress = dayInCycle / 35f

    val phaseName = when {
        homeData.isPeriodActive -> "Periode aktif"
        cycleDay == null -> "Fase siklus"
        cycleDay <= 5 -> "Fase menstruasi"
        cycleDay <= 13 -> "Fase folikuler"
        cycleDay <= 16 -> "Fase ovulasi"
        else -> "Fase luteal"
    }

    val prediction = homeData.prediction
    val nextPredictionRange = if (prediction != null) {
        val startFormatted = prediction.rangeLow.format(DateTimeFormatter.ofPattern("d"))
        val endFormatted = prediction.rangeHigh.format(DateTimeFormatter.ofPattern("d MMMM", Locale("id", "ID")))
        "$startFormatted–$endFormatted"
    } else {
        "Menunggu data"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer concentric cycle ring
        Canvas(modifier = Modifier.size(280.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val strokeWidthPx = 14.dp.toPx()
            val outerRadius = (size.minDimension / 2f) - (strokeWidthPx / 2f)

            // Outer ring base track
            drawCircle(
                color = colors.surfaceSoft,
                radius = outerRadius,
                center = centerOffset,
            )

            // Progress arc
            drawArc(
                color = colors.cyclePrimary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )
        }

        // Inner Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 220.dp),
        ) {
            Text(
                text = if (cycleDay != null) "HARI KE-$cycleDay" else "SIKLUS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.inkSecondary,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = phaseName,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    color = colors.inkPrimary,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "· perkiraan",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    color = colors.inkSecondary,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            // Sub-box "BERIKUTNYA"
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFFFBF3F4),
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "BERIKUTNYA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.inkTertiary,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = nextPredictionRange,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = colors.inkPrimary,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Insight card on home screen with left accent bar and circular badge.
 */
@Composable
fun RonaHomeInsightCard(
    title: String,
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Circular icon badge
            Surface(
                shape = CircleShape,
                color = colors.cycleContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = colors.cyclePrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inkPrimary,
                    ),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.inkSecondary,
                    ),
                )
                if (onClick != null) {
                    Text(
                        text = "Lihat insight →",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.cyclePrimary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun RonaInsightCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val colors = LocalRonaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.titleLarge)
            if (supporting != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "Home Success — light", showBackground = true)
@Composable
private fun HomeSuccessLightPreview() {
    RonaTheme {
        HomeSuccessContent(
            homeData = HomeData(
                cycleDay = 12,
                isPeriodActive = false,
                prediction = CyclePrediction(
                    predictedStart = LocalDate.now().plusDays(16),
                    rangeLow = LocalDate.now().plusDays(14),
                    rangeHigh = LocalDate.now().plusDays(19),
                    medianCycleLengthDays = 28,
                    meanCycleLengthDays = 28.5,
                    madDays = 1.5,
                    cycleCountUsed = 4,
                    confidence = Confidence.MEDIUM,
                ),
                totalPeriods = 4,
                totalLogs = 30,
            ),
            onStartPeriod = {},
            onEndPeriod = {},
            onEditPeriod = { _, _ -> },
            onLogToday = {},
            onOpenCalendar = {},
            onOpenInsights = {},
        )
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(
    name = "Home Empty — Baseline Light (360dp)",
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
)
@Composable
private fun HomeEmptyBaselineLight360Preview() {
    RonaTheme {
        HomeEmptyContent(onStartPeriod = {})
    }
}

@Preview(
    name = "Home Empty — Dark (360dp)",
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeEmptyDark360Preview() {
    RonaTheme(themeMode = ThemeMode.DARK) {
        HomeEmptyContent(onStartPeriod = {})
    }
}

@Preview(
    name = "Home Empty — Narrow (320dp)",
    showBackground = true,
    widthDp = 320,
    heightDp = 700,
)
@Composable
private fun HomeEmptyNarrow320Preview() {
    RonaTheme {
        HomeEmptyContent(onStartPeriod = {})
    }
}

@Preview(
    name = "Home Empty — Typical (411dp)",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun HomeEmptyTypical411Preview() {
    RonaTheme {
        HomeEmptyContent(onStartPeriod = {})
    }
}

@Preview(
    name = "Home Empty — Large Font (1.5x)",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.5f,
)
@Composable
private fun HomeEmptyLargeFont150Preview() {
    RonaTheme {
        HomeEmptyContent(onStartPeriod = {})
    }
}

@Preview(name = "Home Error — light", showBackground = true)
@Composable
private fun HomeErrorLightPreview() {
    RonaTheme {
        RonaErrorState(message = "Datamu tidak bisa dimuat sekarang.", onRetry = {})
    }
}
