package org.mlanau.project.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.figtree_bold
import plantitas_app.shared.generated.resources.figtree_medium
import plantitas_app.shared.generated.resources.figtree_regular
import plantitas_app.shared.generated.resources.figtree_semibold
import plantitas_app.shared.generated.resources.newsreader_italic
import plantitas_app.shared.generated.resources.newsreader_medium
import plantitas_app.shared.generated.resources.newsreader_regular
import plantitas_app.shared.generated.resources.newsreader_semibold

/**
 * Newsreader (a warm text serif) carries the big editorial voice — display, headline and the plant
 * name in [Typography.titleLarge]. Figtree (a humanist sans) does the working text: section labels,
 * body and the small caps-style labels. Both are bundled as resources so Android and iOS render the
 * same thing rather than falling back to Roboto / SF Pro.
 */

@Composable
private fun newsreaderFamily() = FontFamily(
    Font(Res.font.newsreader_regular, FontWeight.Normal),
    Font(Res.font.newsreader_medium, FontWeight.Medium),
    Font(Res.font.newsreader_semibold, FontWeight.SemiBold),
    Font(Res.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic)
)

@Composable
private fun figtreeFamily() = FontFamily(
    Font(Res.font.figtree_regular, FontWeight.Normal),
    Font(Res.font.figtree_medium, FontWeight.Medium),
    Font(Res.font.figtree_semibold, FontWeight.SemiBold),
    Font(Res.font.figtree_bold, FontWeight.Bold)
)

@Composable
fun plantitasTypography(): Typography {
    val serif = newsreaderFamily()
    val sans = figtreeFamily()

    val display = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal)

    return Typography(
        displayLarge = display.copy(fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp),
        displayMedium = display.copy(fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.25).sp),
        displaySmall = display.copy(fontSize = 30.sp, lineHeight = 38.sp),
        headlineLarge = display.copy(fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.25).sp),
        headlineMedium = display.copy(fontSize = 25.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
        headlineSmall = display.copy(fontSize = 21.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
        titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
        titleSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = body.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        bodySmall = body.copy(fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.3.sp),
        labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp)
    )
}
