package com.safetube.app

import androidx.compose.ui.text.platform.Font
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/** কার্ড কেমন সাজানো (চ্যাট অ্যাপের ইউটিউব থিমগুলোর মতো) */
enum class CardLayout { GRID, LIST, CINEMA, MAG, YT }
enum class HeaderStyle { BRAND, YOUTUBE }
enum class Deco { HUE, PLAIN, GOLD, NEON, ARCH, PAPER }

data class YtTheme(
    val id: String,
    val name: String,
    val bg: Color,
    val panel: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val chipOn: Color,
    val header: List<Color>,
    val more: List<Color>,
    val swatch2: Color,
    val layout: CardLayout,
    val deco: Deco,
    val dark: Boolean,
    val headerStyle: HeaderStyle = HeaderStyle.BRAND
)

private fun th(
    id: String, name: String,
    bg: String, panel: String, text: String, muted: String, line: String,
    accent: String, onAccent: String,
    header: List<String>, more: List<String>, swatch2: String,
    layout: CardLayout = CardLayout.GRID, deco: Deco = Deco.HUE,
    dark: Boolean = false, chipOn: String? = null,
    headerStyle: HeaderStyle = HeaderStyle.BRAND
): YtTheme {
    val h = header.map { hx(it) }.let { if (it.size == 1) listOf(it[0], it[0]) else it }
    val m = more.map { hx(it) }.let { if (it.size == 1) listOf(it[0], it[0]) else it }
    return YtTheme(
        id, name, hx(bg), hx(panel), hx(text), hx(muted), hx(line),
        hx(accent), hx(onAccent), hx(chipOn ?: accent), h, m, hx(swatch2), layout, deco, dark, headerStyle
    )
}

val YT_THEMES: List<YtTheme> = listOf(
    th("ytx", "▶ ইউটিউব (হুবহু)", "#ffffff", "#ffffff", "#0f0f0f", "#606060", "#e5e5e5", "#ff0000", "#ffffff",
        listOf("#ffffff"), listOf("#ff0000"), "#ff0000",
        layout = CardLayout.YT, deco = Deco.PLAIN, chipOn = "#0f0f0f", headerStyle = HeaderStyle.YOUTUBE),
    th("ytxd", "▶ ইউটিউব ডার্ক (হুবহু)", "#0f0f0f", "#0f0f0f", "#f1f1f1", "#aaaaaa", "#272727", "#ff0000", "#ffffff",
        listOf("#0f0f0f"), listOf("#ff0000"), "#ff0000",
        layout = CardLayout.YT, deco = Deco.PLAIN, dark = true, chipOn = "#f1f1f1", headerStyle = HeaderStyle.YOUTUBE),
    th("default", "ডিফল্ট", "#f3f5f4", "#ffffff", "#10261d", "#5b7a6c", "#cfe6da", "#0a5f44", "#ffffff",
        listOf("#0a5f44"), listOf("#0f9d8f", "#2563eb"), "#ffffff"),
    th("yt", "ইউটিউব ক্লাসিক", "#f9f9f9", "#ffffff", "#0f0f0f", "#606060", "#e5e5e5", "#ff0000", "#ffffff",
        listOf("#ff0000"), listOf("#ff0000"), "#ffffff", deco = Deco.PLAIN),
    th("dark", "ডার্ক সিনেমা", "#0f0f0f", "#1c1c1c", "#f1f1f1", "#aaaaaa", "#303030", "#2b2b2b", "#ffffff",
        listOf("#1f1f1f", "#0a0a0a"), listOf("#c4302b", "#7f1d1d"), "#1c1c1c", dark = true, chipOn = "#c4302b"),
    th("emerald", "সবুজ মসজিদ", "#f2faf6", "#ffffff", "#10261d", "#5b7a6c", "#cfe6da", "#0a5f44", "#ffffff",
        listOf("#0a5f44", "#16a34a"), listOf("#0a5f44", "#16a34a"), "#16a34a"),
    th("ocean", "নীল সমুদ্র", "#eef6ff", "#ffffff", "#0b2540", "#5a7592", "#cfe0f3", "#1d4ed8", "#ffffff",
        listOf("#1d4ed8", "#06b6d4"), listOf("#1d4ed8", "#06b6d4"), "#06b6d4"),
    th("night", "রাতের আকাশ", "#12102b", "#1c1a3d", "#ece9ff", "#a9a4d6", "#34306b", "#6d28d9", "#ffffff",
        listOf("#4c1d95", "#1e3a8a"), listOf("#6d28d9", "#2563eb"), "#1e3a8a", dark = true),
    th("gold", "সোনালি", "#fffaf0", "#ffffff", "#3b2a0a", "#8a7243", "#f0e1b8", "#b8860b", "#ffffff",
        listOf("#b8860b", "#eab308"), listOf("#b8860b", "#eab308"), "#eab308", deco = Deco.GOLD),
    th("rose", "গোলাপি", "#fff1f4", "#ffffff", "#3a0f1c", "#9a5a6d", "#f6cfd9", "#e11d48", "#ffffff",
        listOf("#e11d48", "#f472b6"), listOf("#e11d48", "#f472b6"), "#f472b6"),
    th("list", "📋 তালিকা (পাশে ছোট ছবি)", "#f3f5f4", "#ffffff", "#10261d", "#5b7a6c", "#cfe6da", "#0a5f44", "#ffffff",
        listOf("#0a5f44"), listOf("#0f9d8f", "#2563eb"), "#ffffff", layout = CardLayout.LIST),
    th("cinema", "🎬 সিনেমা (বড় কার্ড)", "#0a0a0a", "#161616", "#f5f5f5", "#a3a3a3", "#262626", "#e50914", "#ffffff",
        listOf("#1a0a0a", "#000000"), listOf("#e50914"), "#161616", layout = CardLayout.CINEMA, dark = true),
    th("mag", "📰 ম্যাগাজিন (ছবির ওপর লেখা)", "#f6f1e9", "#ffffff", "#1f1b16", "#7a6f62", "#e6dccb", "#b4232c", "#ffffff",
        listOf("#b4232c", "#7a1118"), listOf("#b4232c", "#7a1118"), "#ffffff", layout = CardLayout.MAG),
    th("arch", "🕌 মসজিদ আর্ক (সোনালি)", "#faf6ea", "#fffdf5", "#12301f", "#6b7f6f", "#e3d7a8", "#0a5f44", "#ffffff",
        listOf("#064e3b", "#0a5f44", "#b8860b"), listOf("#0a5f44", "#b8860b"), "#d4a017", deco = Deco.ARCH),
    th("neon", "🌌 নিয়ন গ্লাস", "#070b17", "#0e1426", "#e6f7ff", "#8fb4c9", "#1b2a4a", "#06b6d4", "#00131a",
        listOf("#0b1f3a", "#0e7490"), listOf("#06b6d4", "#8b5cf6"), "#8b5cf6", deco = Deco.NEON, dark = true),
    th("paper", "📜 পুরনো কাগজ", "#f3e9d2", "#fbf4e3", "#3b2a14", "#8a7350", "#d9c7a0", "#8b5a2b", "#fff7e8",
        listOf("#6b4423", "#a47148"), listOf("#8b5a2b"), "#fbf4e3", deco = Deco.PAPER)
)

val LocalYt = compositionLocalOf { YT_THEMES[0] }

/** তিরো বাংলা ফন্ট: resources/fonts/TiroBangla-Regular.ttf থাকলে সেটা, না থাকলে সিস্টেমের ফন্ট (Windows-এ Nirmala UI) */
object AppFont {
    var family: FontFamily = FontFamily.Default

    fun init() {
        try {
            val path = "fonts/TiroBangla-Regular.ttf"
            if (Thread.currentThread().contextClassLoader.getResource(path) != null) {
                family = FontFamily(Font(path))
            }
        } catch (e: Throwable) {
            family = FontFamily.Default
        }
    }
}

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun SafeTubeTheme(t: YtTheme, content: @Composable () -> Unit) {
    val scheme = if (t.dark) {
        darkColorScheme(
            primary = t.chipOn, onPrimary = t.onAccent, background = t.bg, onBackground = t.text,
            surface = t.panel, onSurface = t.text, surfaceVariant = t.line, onSurfaceVariant = t.muted, outline = t.line
        )
    } else {
        lightColorScheme(
            primary = t.chipOn, onPrimary = t.onAccent, background = t.bg, onBackground = t.text,
            surface = t.panel, onSurface = t.text, surfaceVariant = t.line, onSurfaceVariant = t.muted, outline = t.line
        )
    }
    CompositionLocalProvider(LocalYt provides t) {
        ProvideTextStyle(TextStyle(fontFamily = AppFont.family)) {
            MaterialTheme(
                colorScheme = scheme,
                typography = Typography().withFont(AppFont.family),
                content = content
            )
        }
    }
}
