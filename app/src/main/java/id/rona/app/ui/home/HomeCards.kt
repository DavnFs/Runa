package id.rona.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.rona.app.domain.engine.FertilityEstimator
import id.rona.app.domain.insights.CycleEducationTopic
import id.rona.app.domain.model.Confidence
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaMicroLabelStyle
import id.rona.app.ui.theme.RonaPillShape
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID"))
private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

/**
 * Hero "Hari ini" — satu kartu bersih: ring progres kompak di kanan atas,
 * blok prediksi full-width di bawah (gaya Apple: satu permukaan, hierarki
 * tipografi, tanpa kotak bersarang dan tanpa teks yang membungkus sempit).
 */
@Composable
fun RonaTodayCard(
    homeData: HomeData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val prediction = homeData.prediction
    val window = homeData.fertilityWindow

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "HARI INI",
                        style = RonaMicroLabelStyle,
                        color = colors.cyclePrimary,
                    )
                    Text(
                        text = homeData.today.format(fullDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary,
                    )
                    Text(
                        text = homeData.phaseName ?: "Fase siklus",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inkPrimary,
                    )
                }
                CycleDayRing(
                    cycleDay = homeData.cycleDay,
                    modifier = Modifier.size(96.dp),
                )
            }

            HorizontalDivider(color = colors.dividerSubtle)

            if (prediction != null) {
                Text(
                    text = "PERKIRAAN HAID BERIKUTNYA",
                    style = RonaMicroLabelStyle,
                    color = colors.inkTertiary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${prediction.rangeLow.format(dayMonth)}–${prediction.rangeHigh.format(fullDate)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.W700,
                        ),
                        color = colors.inkPrimary,
                    )
                    ConfidencePill(prediction.confidence)
                }
                homeData.daysUntilNextPeriod?.let { days ->
                    Text(
                        text = when {
                            days == 0 -> "Perkiraan haid dimulai hari ini."
                            days > 0 -> "Sekitar $days hari lagi."
                            else -> "Lewat ${-days} hari dari perkiraan."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary,
                    )
                }
            } else {
                Text(
                    text = "Tambahkan satu periode berikutnya agar Runa bisa membuat perkiraan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary,
                )
            }

            if (window != null && FertilityEstimator.isInWindow(homeData.today, window)) {
                Text(
                    text = "Masa subur: ${window.low.format(dayMonth)}–${window.high.format(fullDate)} · estimasi kalender",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.cyclePrimary,
                    ),
                )
            }
        }
    }
}

/** Ring progres kompak dengan nomor hari di tengah. */
@Composable
private fun CycleDayRing(
    cycleDay: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val day = (cycleDay ?: 0).coerceIn(0, 35)
    val progress = day / 35f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 9.dp.toPx()
            val outerRadius = (size.minDimension / 2f) - (strokeWidthPx / 2f)
            drawCircle(
                color = colors.elevatedSurface,
                radius = outerRadius,
                style = Stroke(width = strokeWidthPx),
            )
            if (progress > 0f) {
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
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cycleDay?.toString() ?: "–",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.W800,
                ),
                color = colors.inkPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "HARI KE",
                style = RonaMicroLabelStyle,
                color = colors.inkTertiary,
            )
        }
    }
}

/**
 * Kartu wawasan harian: satu topik edukasi sesuai fase hari ini.
 */
@Composable
fun RonaDailyInsightCard(
    topic: CycleEducationTopic,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "WAWASAN HARIANMU",
                style = RonaMicroLabelStyle,
                color = colors.cyclePrimary,
            )
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.W700,
                    fontSize = 18.sp,
                ),
                color = colors.inkPrimary,
            )
            Text(
                text = topic.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
            )
            Text(
                text = "Baca di Insight →",
                style = MaterialTheme.typography.labelLarge,
                color = colors.cyclePrimary,
                modifier = Modifier.clickable(onClick = onOpenInsights),
            )
        }
    }
}

@Composable
private fun ConfidencePill(confidence: Confidence) {
    val colors = LocalRonaColors.current
    val label = when (confidence) {
        Confidence.HIGH -> "Estimasi tinggi"
        Confidence.MEDIUM -> "Estimasi sedang"
        Confidence.LOW -> "Estimasi awal"
    }
    Surface(
        shape = RonaPillShape,
        color = colors.cycleContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onCycleContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
