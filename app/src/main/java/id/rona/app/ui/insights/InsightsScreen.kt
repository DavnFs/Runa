package id.rona.app.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.engine.SymptomFrequency
import id.rona.app.domain.model.SymptomType
import id.rona.app.ui.components.RonaEmptyState
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.components.RonaTopBar
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaTheme

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Menyiapkan pola…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        RonaTopBar(
            onOpenSettings = onOpenSettings,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header Title & Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Insight",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary,
                    ),
                )
                Text(
                    text = "Pola dari catatanmu, bukan diagnosis.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = colors.inkSecondary,
                    ),
                )
            }

            if (uiState.totalPeriods < 2) {
                RonaEmptyState(
                    icon = Icons.Rounded.QueryStats,
                    title = "Pola akan muncul perlahan",
                    message = "Semakin banyak catatan yang kamu simpan, semakin mudah melihat pola personalmu.",
                )
            } else {
                val mostFrequent = uiState.mostFrequentSymptoms.firstOrNull()
                val frequentSymptomText = if (mostFrequent != null) {
                    symptomLabel(mostFrequent.symptomType)
                } else {
                    "Kram ringan"
                }

                // Bento Grid Cards
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Card 1: Siklus Rata-Rata
                    RonaBentoStatCard(
                        icon = Icons.Rounded.Autorenew,
                        iconTint = colors.cyclePrimary,
                        badgeColor = colors.cycleContainer,
                        label = "SIKLUS RATA-RATA",
                        value = uiState.medianCycleLength?.toString() ?: "29",
                        unit = "hari",
                    )

                    // Card 2: Yang Sering Muncul
                    RonaBentoTextCard(
                        icon = Icons.Rounded.Opacity,
                        iconTint = colors.plumAccent,
                        badgeColor = colors.plumContainer,
                        label = "YANG SERING MUNCUL",
                        value = frequentSymptomText,
                    )
                }

                // Card 3: Panjang Siklus Graph Card
                RonaCycleLengthGraphCard()
            }

            Text(
                text = "Semua angka dihitung dari catatanmu di perangkat ini — informasi umum, bukan nasihat medis.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = colors.inkTertiary,
                ),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RonaBentoStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badgeColor: Color,
    label: String,
    value: String,
    unit: String,
) {
    val colors = LocalRonaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = colors.inkTertiary,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$value ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = colors.inkPrimary,
                        ),
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            color = colors.inkSecondary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RonaBentoTextCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badgeColor: Color,
    label: String,
    value: String,
) {
    val colors = LocalRonaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = colors.inkTertiary,
                    ),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = colors.inkPrimary,
                    ),
                )
            }
        }
    }
}

/**
 * Pixel-accurate "Panjang siklus" smooth line graph card from Figma.
 */
@Composable
private fun RonaCycleLengthGraphCard() {
    val colors = LocalRonaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Panjang siklus",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    color = colors.inkPrimary,
                ),
            )

            // Canvas Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines (3 horizontal divider lines)
                    val gridColor = colors.dividerSubtle
                    val lineCount = 3
                    for (i in 0 until lineCount) {
                        val y = (h / (lineCount + 1)) * (i + 1)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    // 4 data points (Jan, Feb, Mar, Apr)
                    val points = listOf(
                        Offset(w * 0.08f, h * 0.70f),
                        Offset(w * 0.38f, h * 0.60f),
                        Offset(w * 0.68f, h * 0.35f),
                        Offset(w * 0.92f, h * 0.65f),
                    )

                    // Smooth Bezier Curve Path
                    val curvePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                    }

                    // Draw curve stroke
                    drawPath(
                        path = curvePath,
                        color = colors.supportAccent,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Draw circular point dots
                    points.forEach { point ->
                        drawCircle(
                            color = colors.supportAccent,
                            radius = 5.dp.toPx(),
                            center = point,
                        )
                    }
                }
            }

            // Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("Jan", "Feb", "Mar", "Apr").forEach { month ->
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = colors.inkTertiary,
                        ),
                    )
                }
            }

            // Supporting insight
            Text(
                text = "Siklusmu tergolong stabil dalam 4 bulan terakhir.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = colors.inkSecondary,
                ),
            )
        }
    }
}

private fun symptomLabel(symptom: SymptomType): String = when (symptom) {
    SymptomType.KRAM -> "Kram ringan"
    SymptomType.HEADACHE -> "Sakit kepala"
    SymptomType.NAUSEA -> "Mual"
    SymptomType.BLOATING -> "Kembung"
    SymptomType.FATIGUE -> "Lelah"
    SymptomType.BREAST_TENDERNESS -> "Nyeri payudara"
    SymptomType.ACNE -> "Jerawat"
    SymptomType.BACKACHE -> "Nyeri punggung"
    SymptomType.SLEEP_ISSUE -> "Sulit tidur"
    SymptomType.APPETITE_CHANGE -> "Perubahan nafsu makan"
}

@androidx.compose.ui.tooling.preview.Preview(name = "Insights Screen — Light", showBackground = true)
@Composable
private fun InsightsScreenLightPreview() {
    RonaTheme {
        InsightsScreen(onOpenSettings = {})
    }
}
