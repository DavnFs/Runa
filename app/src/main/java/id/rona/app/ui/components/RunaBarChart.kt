package id.rona.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.rona.app.ui.theme.LocalRonaColors
import kotlin.math.max

/**
 * Bar chart of cycle lengths, drawn with plain Canvas (no chart library).
 * Bars carry their value above them, x labels are month abbreviations, and the
 * median is shown as a dashed reference line.
 */
@Composable
fun RonaBarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp,
    median: Int? = null,
) {
    val colors = LocalRonaColors.current
    val textMeasurer = rememberTextMeasurer()

    if (values.size < 3) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Butuh minimal 3 siklus untuk grafik.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkTertiary,
            )
        }
        return
    }

    val valueStyle = TextStyle(
        fontSize = 10.sp,
        color = colors.inkSecondary,
    )
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = colors.inkTertiary,
    )
    val medianStyle = TextStyle(
        fontSize = 10.sp,
        color = colors.cyclePrimary,
    )

    val trendSummary = "Panjang siklus terakhir: " + values.joinToString(", ") + " hari."

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .semantics { contentDescription = trendSummary },
    ) {
        val topPad = 36.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val sidePad = 10.dp.toPx()

        val minVal = values.minOrNull() ?: return@Canvas
        val maxVal = values.maxOrNull() ?: return@Canvas
        val lo = (minVal - 1).toFloat()
        val hi = (maxVal + 1).toFloat()

        val plotWidth = size.width - sidePad * 2
        val plotHeight = size.height - topPad - bottomPad
        val slot = plotWidth / values.size
        val barWidth = slot * 0.55f

        fun yOf(value: Int): Float =
            topPad + plotHeight * (1f - (value - lo) / (hi - lo))

        fun xOf(index: Int): Float = sidePad + slot * index + slot / 2f

        // Horizontal grid lines (3 inner levels).
        for (i in 1..3) {
            val y = topPad + plotHeight * i / 4f
            drawLine(
                color = colors.dividerSubtle,
                start = Offset(sidePad, y),
                end = Offset(size.width - sidePad, y),
                strokeWidth = 1f,
            )
        }

        // Median reference line.
        median?.let { med ->
            val y = yOf(med.coerceIn(minVal, maxVal))
            drawLine(
                color = colors.cyclePrimary.copy(alpha = 0.55f),
                start = Offset(sidePad, y),
                end = Offset(size.width - sidePad, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
            // Legend sits in its own row above the plot so it never collides
            // with a bar's value label.
            val legendText = "median $med"
            val legendLayout = textMeasurer.measure(legendText, medianStyle)
            val legendY = 2.dp.toPx()
            drawText(
                textLayoutResult = legendLayout,
                topLeft = Offset(sidePad + 22.dp.toPx(), legendY),
            )
            drawLine(
                color = colors.cyclePrimary.copy(alpha = 0.55f),
                start = Offset(sidePad, legendY + legendLayout.size.height / 2f),
                end = Offset(sidePad + 16.dp.toPx(), legendY + legendLayout.size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
        }

        // Bars, value labels, and x labels.
        val labelEvery = if (values.size > 8) 2 else 1
        values.forEachIndexed { index, value ->
            val cx = xOf(index)
            val top = yOf(value)
            val baseline = size.height - bottomPad

            drawRoundRect(
                color = colors.cyclePrimary,
                topLeft = Offset(cx - barWidth / 2f, top),
                size = Size(barWidth, max(2.dp.toPx(), baseline - top)),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            val valueLayout = textMeasurer.measure(value.toString(), valueStyle)
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(
                    (cx - valueLayout.size.width / 2f).coerceIn(0f, size.width - valueLayout.size.width),
                    max(0f, top - valueLayout.size.height - 4.dp.toPx()),
                ),
            )

            if (index % labelEvery == 0 && index < labels.size) {
                val labelLayout = textMeasurer.measure(labels[index], labelStyle)
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(
                        (cx - labelLayout.size.width / 2f).coerceIn(0f, size.width - labelLayout.size.width),
                        size.height - bottomPad + 4.dp.toPx(),
                    ),
                )
            }
        }
    }
}
