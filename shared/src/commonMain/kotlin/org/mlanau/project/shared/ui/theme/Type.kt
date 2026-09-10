package org.mlanau.project.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.bricolage_grotesque_bold
import plantitas_app.shared.generated.resources.bricolage_grotesque_extrabold
import plantitas_app.shared.generated.resources.figtree_bold
import plantitas_app.shared.generated.resources.figtree_medium
import plantitas_app.shared.generated.resources.figtree_regular
import plantitas_app.shared.generated.resources.figtree_semibold

/**
 * Bricolage Grotesque (a chunky, characterful grotesque) carries the display voice — screen titles,
 * plant names and block headings. Figtree (a humanist sans) does the working text. Both are bundled
 * as resources so Android and iOS render the same thing rather than falling back to Roboto / SF Pro.
 */

@Composable
private fun bricolageFamily() = FontFamily(
    Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(Res.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold)
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
    val display = TextStyle(fontFamily = bricolageFamily(), fontWeight = FontWeight.ExtraBold)
    val sans = figtreeFamily()
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold)

    return Typography(
        displayLarge = display.copy(fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1.1).sp),
        displayMedium = display.copy(fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.9).sp),
        displaySmall = display.copy(fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.75).sp),
        headlineLarge = display.copy(fontSize = 33.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
        headlineMedium = display.copy(fontSize = 25.sp, lineHeight = 29.sp, letterSpacing = (-0.6).sp),
        headlineSmall = display.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.5).sp),
        titleLarge = display.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
        titleMedium = label.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        titleSmall = label.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = body.copy(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
        bodyMedium = body.copy(fontSize = 13.5.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
        bodySmall = body.copy(fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp),
        labelLarge = label.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = label.copy(fontSize = 12.5.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
        labelSmall = label.copy(fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 1.1.sp)
    )
}
