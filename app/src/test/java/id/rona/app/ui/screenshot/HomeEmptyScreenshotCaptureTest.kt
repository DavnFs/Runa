package id.rona.app.ui.screenshot

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.components.RunaDockDestination
import id.rona.app.ui.components.RunaFloatingNavDock
import id.rona.app.ui.components.RunaTopBar
import id.rona.app.ui.components.runaPageContainer
import id.rona.app.ui.home.HomeEmptyContent
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RunaTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeEmptyScreenshotCaptureTest {

    /** Page margin (20dp) in pixels at the 2.75 density these captures use. */
    private val pageMarginPx = (20 * 2.75f).toInt()

    private val screenshotsDir by lazy {
        val base = System.getProperty("user.dir")?.let { File(it) } ?: File(".")
        val dir = if (base.name == "app") File(base.parentFile, "screenshots") else File(base, "screenshots")
        dir.apply { mkdirs() }
    }

    private fun renderComposableToBitmap(
        widthDp: Int,
        heightDp: Int,
        densityVal: Float = 2.75f,
        fontScaleVal: Float = 1.0f,
        content: @Composable () -> Unit,
    ): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val widthPx = (widthDp * densityVal).toInt()
        val heightPx = (heightDp * densityVal).toInt()

        val composeView = ComposeView(activity).apply {
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = densityVal, fontScale = fontScaleVal),
                ) {
                    content()
                }
            }
        }

        activity.setContentView(composeView)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, widthPx, heightPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap, filename: String, toBuildDir: Boolean = false) {
        runCatching {
            val dir = if (toBuildDir) {
                val base = System.getProperty("user.dir")?.let { File(it) } ?: File(".")
                val app = if (base.name == "app") base else File(base, "app")
                File(app, "build/probe-ui").apply { mkdirs() }
            } else {
                screenshotsDir
            }
            val fileInRepo = File(dir, filename)
            fileInRepo.parentFile?.mkdirs()
            FileOutputStream(fileInRepo).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("Saved screenshot: ${fileInRepo.absolutePath}")
        }
    }

    @Test
    fun captureHomeEmptyLight360() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 780) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RunaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-light-360.png")
    }

    @Test
    fun captureHomeEmptyDark360() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 780) {
            RunaTheme(themeMode = ThemeMode.DARK) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RunaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-dark-360.png")
    }

    @Test
    fun captureHomeEmptyLight320() {
        val bitmap = renderComposableToBitmap(widthDp = 320, heightDp = 700) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RunaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-light-320.png")
    }

    @Test
    fun captureHomeEmptyLight411() {
        val bitmap = renderComposableToBitmap(widthDp = 411, heightDp = 891) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RunaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-light-411.png")
    }

    @Test
    fun captureHomeEmptyFont150() {
        val bitmap = renderComposableToBitmap(
            widthDp = 360,
            heightDp = 820,
            fontScaleVal = 1.5f,
        ) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RunaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-font150.png")
    }

    @Test
    fun captureDockHomeSelected() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 100) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.Center,
                ) {
                    RunaFloatingNavDock(
                        selected = RunaDockDestination.HOME,
                        onDestinationSelected = {},
                    )
                }
            }
        }
        saveBitmap(bitmap, "ui-dock-home-selected.png")
    }

    @Test
    fun captureDockHomeSelectedDark() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 100) {
            RunaTheme(themeMode = ThemeMode.DARK) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.Center,
                ) {
                    RunaFloatingNavDock(
                        selected = RunaDockDestination.HOME,
                        onDestinationSelected = {},
                    )
                }
            }
        }
        saveBitmap(bitmap, "ui-dock-home-selected-dark.png")
    }

    @Test
    fun captureHomeSuccessLight360() {
        val today = java.time.LocalDate.now()
        val prediction = id.rona.app.domain.engine.CyclePrediction(
            predictedStart = today.plusDays(16),
            rangeLow = today.plusDays(14),
            rangeHigh = today.plusDays(19),
            medianCycleLengthDays = 28,
            meanCycleLengthDays = 28.5,
            madDays = 1.5,
            cycleCountUsed = 4,
            confidence = id.rona.app.domain.model.Confidence.MEDIUM,
        )
        val homeData = id.rona.app.ui.home.HomeData(
            today = today,
            cycleDay = 12,
            isPeriodActive = false,
            prediction = prediction,
            daysUntilNextPeriod = 16,
            fertilityWindow = id.rona.app.domain.engine.FertilityEstimator.estimate(prediction),
            phaseName = id.rona.app.domain.engine.phaseNameFor(false, 12),
            dailyInsight = id.rona.app.domain.insights.CycleEducationProvider.phaseTopicForToday(
                12, 28, id.rona.app.domain.insights.InsightMaturityLevel.LEVEL_4_MATURE,
            ),
            totalPeriods = 4,
            totalLogs = 30,
        )
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 780) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .runaPageContainer()
                            .fillMaxSize(),
                    ) {
                        RunaTopBar(
                            subtitle = today.format(
                                java.time.format.DateTimeFormatter.ofPattern(
                                    "EEEE, d MMMM",
                                    java.util.Locale("id", "ID"),
                                ),
                            ),
                            onOpenSettings = {},
                        )
                        id.rona.app.ui.home.HomeContentColumn {
                            id.rona.app.ui.home.HomeSuccessContent(
                                homeData = homeData,
                                onStartPeriod = {},
                                onEndPeriod = {},
                                onEditPeriod = { _, _ -> },
                                onLogToday = {},
                                onOpenCalendar = {},
                                onOpenInsights = {},
                            )
                        }
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-success-light-360.png")
        assertCardsInset(bitmap)
    }

    /**
     * Guards the regression that made screenshots look edge-to-edge: page
     * content must start at [pageMarginPx], never at x = 0.
     */
    private fun assertCardsInset(bitmap: Bitmap) {
        val midY = bitmap.height / 2
        val leftPx = (0 until bitmap.width).firstOrNull {
            sumRgb(bitmap.getPixel(it, midY)) < 740
        }
        org.junit.Assert.assertNotNull("no content found at mid height", leftPx)
        org.junit.Assert.assertTrue(
            "content starts at x=$leftPx but the page margin is ~${pageMarginPx}px",
            leftPx!! >= pageMarginPx - 4,
        )
    }

    private fun sumRgb(color: Int): Int =
        android.graphics.Color.red(color) +
            android.graphics.Color.green(color) +
            android.graphics.Color.blue(color)

    @Test
    fun captureHomeEmptyWide600() {
        val bitmap = renderComposableToBitmap(widthDp = 600, heightDp = 800) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .runaPageContainer()
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        RunaTopBar(onOpenSettings = {})
                        HomeEmptyContent(onStartPeriod = {})
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-wide-600.png")
    }

    @Test
    fun captureHomeEmptyLandscape() {
        val bitmap = renderComposableToBitmap(widthDp = 640, heightDp = 360) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .runaPageContainer()
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        RunaTopBar(onOpenSettings = {})
                        HomeEmptyContent(onStartPeriod = {})
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-landscape-640x360.png")
    }

    @Test
    fun captureHomeEmptyFont200() {
        val bitmap = renderComposableToBitmap(
            widthDp = 360,
            heightDp = 800,
            fontScaleVal = 2.0f,
        ) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .runaPageContainer()
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        RunaTopBar(onOpenSettings = {})
                        HomeEmptyContent(onStartPeriod = {})
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        RunaFloatingNavDock(
                            selected = RunaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    }
                }
            }
        }
        saveBitmap(bitmap, "ui-home-empty-font200.png")
    }

    /**
     * Diagnostic capture of the Today card on its own — writes to the build
     * directory, not `screenshots/`, since it is a dev artifact rather than a
     * product screenshot.
     */
    @Test
    fun captureTodayCardIsolated() {
        val today = java.time.LocalDate.now()
        val prediction = id.rona.app.domain.engine.CyclePrediction(
            predictedStart = today.plusDays(16),
            rangeLow = today.plusDays(14),
            rangeHigh = today.plusDays(19),
            medianCycleLengthDays = 28,
            meanCycleLengthDays = 28.5,
            madDays = 1.5,
            cycleCountUsed = 4,
            confidence = id.rona.app.domain.model.Confidence.MEDIUM,
        )
        val homeData = id.rona.app.ui.home.HomeData(
            today = today,
            cycleDay = 12,
            isPeriodActive = false,
            prediction = prediction,
            daysUntilNextPeriod = 16,
            fertilityWindow = id.rona.app.domain.engine.FertilityEstimator.estimate(prediction),
            phaseName = id.rona.app.domain.engine.phaseNameFor(false, 12),
        )
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 400) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas)
                        .padding(20.dp),
                ) {
                    id.rona.app.ui.home.RunaTodayCard(homeData = homeData)
                }
            }
        }
        saveBitmap(bitmap, "ui-today-card-isolated.png", toBuildDir = true)
    }

    @Test
    fun captureTopBarLight() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 90) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    RunaTopBar(subtitle = "Kamis, 10 September", onOpenSettings = {})
                }
            }
        }
        saveBitmap(bitmap, "ui-topbar-light.png")
    }

    @Test
    fun captureTopBarDark() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 90) {
            RunaTheme(themeMode = ThemeMode.DARK) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    RunaTopBar(subtitle = "Kamis, 10 September", onOpenSettings = {})
                }
            }
        }
        saveBitmap(bitmap, "ui-topbar-dark.png")
    }

    @Test
    fun captureCycleChartSample() {
        val bitmap = renderComposableToBitmap(widthDp = 360, heightDp = 300) {
            RunaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas)
                        .padding(20.dp),
                ) {
                    id.rona.app.ui.components.RunaBarChart(
                        values = listOf(28, 30, 27, 31, 29, 28, 33, 28),
                        labels = listOf("Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep"),
                        median = 28,
                    )
                }
            }
        }
        saveBitmap(bitmap, "ui-cycle-chart-sample.png")
    }
}
