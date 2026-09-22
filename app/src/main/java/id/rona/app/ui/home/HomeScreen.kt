package id.rona.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import id.rona.app.ui.components.RunaEmptyState
import id.rona.app.ui.components.RunaErrorState
import id.rona.app.ui.components.RunaLoadingSkeleton
import id.rona.app.ui.components.RunaTopBar
import id.rona.app.ui.components.runaHeroDiameter
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RunaMotion
import id.rona.app.ui.theme.RunaPillShape
import id.rona.app.ui.theme.RunaSpacing
import id.rona.app.ui.theme.RunaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The padded, gapped column every Home state renders into. Shared with the
 * screenshot tests so the captured layout cannot drift from the real screen —
 * an earlier harness skipped this padding and made cards look edge-to-edge.
 */
@Composable
internal fun HomeContentColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunaSpacing.screenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(RunaSpacing.cardGap),
        content = content,
    )
}

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
        RunaTopBar(
            subtitle = todayFormatted,
            onOpenSettings = onOpenSettings,
        )

        HomeContentColumn {
            when (val state = uiState) {
                is HomeUiState.Loading -> RunaLoadingSkeleton(message = "Menyiapkan halamanmu…")
                is HomeUiState.Empty -> HomeEmptyContent(
                    onStartPeriod = {
                        editingPeriodId = null
                        initialDateForPeriod = LocalDate.now()
                        showEditPeriodSheet = true
                    },
                )
                is HomeUiState.Error -> RunaErrorState(
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
            shape = id.rona.app.ui.theme.RunaBottomSheetShape,
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
fun RunaEmptyHeroCircle(modifier: Modifier = Modifier) {
    val colors = LocalRonaColors.current
    val diameter = runaHeroDiameter(236.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = if (isPressed) 0.97f else 1f

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RunaPillShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
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
                    color = MaterialTheme.colorScheme.onPrimary,
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
        RunaEmptyHeroCircle()

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
internal fun HomeSuccessContent(
    homeData: HomeData,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit,
    onEditPeriod: (Long?, LocalDate) -> Unit,
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val colors = LocalRonaColors.current

    // 1. Hero "Hari ini": cycle day, phase, next-period prediction, fertile window.
    RunaTodayCard(homeData = homeData)

    // 2. Primary Action Button: Pill button matching Figma
    RunaPrimaryPillButton(
        text = "Catat keadaanmu hari ini",
        onClick = onLogToday,
        modifier = Modifier.fillMaxWidth(),
    )

    // 3. Daily phase-aware insight.
    homeData.dailyInsight?.let { topic ->
        RunaDailyInsightCard(
            topic = topic,
            onOpenInsights = onOpenInsights,
        )
    }

    // 4. Quick Action Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (homeData.isPeriodActive) {
            id.rona.app.ui.components.RunaSecondaryButton(
                text = "Selesaikan periode",
                onClick = onEndPeriod,
                modifier = Modifier.weight(1f),
            )
        } else {
            id.rona.app.ui.components.RunaSecondaryButton(
                text = "Mulai periode",
                onClick = onStartPeriod,
                modifier = Modifier.weight(1f),
            )
        }
        if (homeData.latestPeriod != null) {
            id.rona.app.ui.components.RunaSecondaryButton(
                text = "Edit periode",
                onClick = { onEditPeriod(homeData.latestPeriod.id, homeData.latestPeriod.startDate) },
                modifier = Modifier.weight(1f),
            )
        } else {
            id.rona.app.ui.components.RunaSecondaryButton(
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

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "Home Success — light", showBackground = true)
@Composable
private fun HomeSuccessLightPreview() {
    RunaTheme {
        val prediction = CyclePrediction(
            predictedStart = LocalDate.now().plusDays(16),
            rangeLow = LocalDate.now().plusDays(14),
            rangeHigh = LocalDate.now().plusDays(19),
            medianCycleLengthDays = 28,
            meanCycleLengthDays = 28.5,
            madDays = 1.5,
            cycleCountUsed = 4,
            confidence = Confidence.MEDIUM,
        )
        HomeSuccessContent(
            homeData = HomeData(
                cycleDay = 12,
                isPeriodActive = false,
                prediction = prediction,
                daysUntilNextPeriod = 16,
                fertilityWindow = id.rona.app.domain.engine.FertilityEstimator.estimate(prediction),
                phaseName = id.rona.app.domain.engine.phaseNameFor(false, 12),
                dailyInsight = id.rona.app.domain.insights.CycleEducationProvider
                    .phaseTopicForToday(12, 28, id.rona.app.domain.insights.InsightMaturityLevel.LEVEL_4_MATURE),
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
    RunaTheme {
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
    RunaTheme(themeMode = ThemeMode.DARK) {
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
    RunaTheme {
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
    RunaTheme {
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
    RunaTheme {
        HomeEmptyContent(onStartPeriod = {})
    }
}

@Preview(name = "Home Error — light", showBackground = true)
@Composable
private fun HomeErrorLightPreview() {
    RunaTheme {
        RunaErrorState(message = "Datamu tidak bisa dimuat sekarang.", onRetry = {})
    }
}
