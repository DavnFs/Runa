package id.rona.app.ui.screenshot

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.components.LocalRonaHazeState
import id.rona.app.ui.components.RonaDockDestination
import id.rona.app.ui.components.RonaFloatingNavDock
import id.rona.app.ui.components.RonaTopBar
import id.rona.app.ui.home.HomeEmptyContent
import id.rona.app.ui.theme.RonaTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Structural probe for the main shell: Scaffold + bottomBar dock + haze source.
 * The per-screen captures use a plain Column, so they never exercised this
 * layout. The body is painted red: if the centre of the output is not red, the
 * content is being covered or pushed out by the bar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainShellProbeTest {

    private val outDir by lazy {
        val base = File(System.getProperty("user.dir") ?: ".")
        val app = if (base.name == "app") base else File(base, "app")
        File(app, "build/probe-shell").apply { mkdirs() }
    }

    private fun render(
        widthDp: Int,
        heightDp: Int,
        content: @Composable () -> Unit,
    ): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val densityVal = 2.75f
        val widthPx = (widthDp * densityVal).toInt()
        val heightPx = (heightDp * densityVal).toInt()
        val view = ComposeView(activity).apply {
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = densityVal, fontScale = 1f),
                ) { content() }
            }
        }
        activity.setContentView(view)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun save(bitmap: Bitmap, name: String) {
        FileOutputStream(File(outDir, name)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun probeScaffoldWithDockAndHaze() {
        val bitmap = render(360, 780) {
            val haze = remember { HazeState() }
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalRonaHazeState provides haze) {
                    Scaffold(
                        containerColor = Color.White,
                        bottomBar = {
                            RonaFloatingNavDock(
                                selected = RonaDockDestination.HOME,
                                onDestinationSelected = {},
                            )
                        },
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .hazeSource(haze)
                                .fillMaxSize()
                                .background(Color.Red),
                        )
                    }
                }
            }
        }
        save(bitmap, "probe-shell-scaffold.png")
        val w = bitmap.width
        val h = bitmap.height
        // The dock's glass layer must never grow past the pill: if it fills the
        // screen it paints over the page and the app shows nothing but the bar.
        org.junit.Assert.assertEquals(
            "dock covered the page content",
            android.graphics.Color.RED,
            bitmap.getPixel(w / 2, h / 2),
        )
    }

    @Test
    fun probeScaffoldWithDockNoHaze() {
        val bitmap = render(360, 780) {
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                Scaffold(
                    containerColor = Color.White,
                    bottomBar = {
                        RonaFloatingNavDock(
                            selected = RonaDockDestination.HOME,
                            onDestinationSelected = {},
                        )
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(Color.Red),
                    )
                }
            }
        }
        save(bitmap, "probe-shell-nohaze.png")
        val w = bitmap.width
        val h = bitmap.height
        println("PROBE-NOHAZE centre(px)=" + bitmap.getPixel(w / 2, h / 2))
        println("PROBE-NOHAZE upper(px)=" + bitmap.getPixel(w / 2, h / 4))
    }

    /** Mirrors MainScreen: Scaffold + dock bottomBar + scrollable screen with top bar. */
    @Test
    fun probeMainScreenStructure() {
        val bitmap = render(360, 780) {
            val haze = remember { HazeState() }
            RonaTheme(themeMode = ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalRonaHazeState provides haze) {
                    Scaffold(
                        containerColor = Color.White,
                        bottomBar = {
                            RonaFloatingNavDock(
                                selected = RonaDockDestination.HOME,
                                onDestinationSelected = {},
                            )
                        },
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .hazeSource(haze)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            RonaTopBar(onOpenSettings = {})
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                HomeEmptyContent(onStartPeriod = {})
                            }
                        }
                    }
                }
            }
        }
        save(bitmap, "probe-main-structure.png")
    }
}
