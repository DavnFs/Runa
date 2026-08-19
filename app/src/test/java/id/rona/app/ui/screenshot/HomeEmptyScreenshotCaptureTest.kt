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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.components.RonaDockDestination
import id.rona.app.ui.components.RonaFloatingNavDock
import id.rona.app.ui.components.RonaTopBar
import id.rona.app.ui.home.HomeEmptyContent
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaTheme
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

    private fun saveBitmap(bitmap: Bitmap, filename: String) {
        runCatching {
            val fileInRepo = File(screenshotsDir, filename)
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
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RonaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RonaFloatingNavDock(
                            selected = RonaDockDestination.HOME,
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
            RonaTheme(themeMode = ThemeMode.DARK) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RonaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RonaFloatingNavDock(
                            selected = RonaDockDestination.HOME,
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
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RonaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RonaFloatingNavDock(
                            selected = RonaDockDestination.HOME,
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
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RonaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RonaFloatingNavDock(
                            selected = RonaDockDestination.HOME,
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
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RonaTopBar(onOpenSettings = {})
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeEmptyContent(onStartPeriod = {})
                        }
                        RonaFloatingNavDock(
                            selected = RonaDockDestination.HOME,
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
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalRonaColors.current.pageCanvas),
                    contentAlignment = Alignment.Center,
                ) {
                    RonaFloatingNavDock(
                        selected = RonaDockDestination.HOME,
                        onDestinationSelected = {},
                    )
                }
            }
        }
        saveBitmap(bitmap, "ui-dock-home-selected.png")
    }
}
