package com.wavvy.app.core.designsystem.icons

// UI graphics and vectors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// House
private const val HomePath = "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"

// Magnifying glass
private const val ExplorePath =
    "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16" +
        "c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5" +
        "S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"

// Globe outline with the continents cut out
private const val DiscoverPath =
    "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM11,19.93c-3.95,-0.49 " +
        "-7,-3.85 -7,-7.93 0,-0.62 0.08,-1.21 0.21,-1.79L9,15v1c0,1.1 0.9,2 2,2v1.93zM17.9,17.39c" +
        "-0.26,-0.81 -1,-1.39 -1.9,-1.39h-1v-3c0,-0.55 -0.45,-1 -1,-1L8,12v-2h2c0.55,0 1,-0.45 " +
        "1,-1L11,7h2c1.1,0 2,-0.9 2,-2v-0.41c2.93,1.19 5,4.06 5,7.41 0,2.08 -0.8,3.97 -2.1,5.39z"

// Head and shoulders
private const val PersonPath =
    "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"

// Bell outline
private const val BellPath =
    "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5" +
        "s-1.5,0.67 -1.5,1.5v0.68C7.64,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2zM16,17H8v-6c0,-2.48 1.51,-4.5 4,-4.5s4,2.02 4,4.5v6z"

// Arrow pointing left
private const val BackPath = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"

// Arrow going into a door, drawn on a 32 unit grid
private const val LoginPath =
    "M12.219,26.156h6.094c1.156,0 2.125,-0.406 2.875,-1.188 0.75,-0.75 1.219,-1.75 1.219,-2.875v-12.219c0,-1.125 -0.469,-2.125 -1.219,-2.875s-1.75,-1.188 -2.875,-1.188h-6.094v2.563h6.094c0.875,0 1.531,0.656 1.531,1.5v12.219c0,0.844 -0.656,1.531 -1.531,1.531h-6.094v2.531z" +
        "M0,13.563v4.875c0,0.563 0.469,1.031 1.031,1.031h5.688v3.844c0,0.344 0.156,0.625 0.469,0.781 0.125,0.031 0.281,0.031 0.344,0.031 0.219,0 0.406,-0.063 0.563,-0.219l7.344,-7.344c0.281,-0.281 0.25,-0.844 0,-1.156l-7.344,-7.313c-0.25,-0.25 -0.563,-0.281 -0.906,-0.188 -0.313,0.156 -0.469,0.406 -0.469,0.75v3.875h-5.688c-0.563,0 -1.031,0.469 -1.031,1.031z"
private const val LoginGrid = 32f

// Puzzle piece
private const val IntegrationsPath =
    "M20.5,11H19V7c0,-1.1 -0.9,-2 -2,-2h-4V3.5C13,2.12 11.88,1 10.5,1S8,2.12 8,3.5V5H4c-1.1,0 -1.99,0.9 -1.99,2v3.8H3.5" +
        "c1.49,0 2.7,1.21 2.7,2.7s-1.21,2.7 -2.7,2.7H2V20c0,1.1 0.9,2 2,2h3.8v-1.5c0,-1.49 1.21,-2.7 2.7,-2.7 1.49,0 2.7,1.21 " +
        "2.7,2.7V22H17c1.1,0 2,-0.9 2,-2v-4h1.5c1.38,0 2.5,-1.12 2.5,-2.5S21.88,11 20.5,11z"

// Gear
private const val SettingsPath =
    "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61" +
        "l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 " +
        "-0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 " +
        "-0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l" +
        "-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94" +
        "l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96" +
        "c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6" +
        "s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z"

// Door with an arrow going out, drawn with a line on a 512 unit grid
private const val SignOutPath =
    "M340,120 V100 A80,80 0 0 0 260,20 H100 A80,80 0 0 0 20,100 V412 A80,80 0 0 0 100,492 H260 A80,80 0 0 0 340,412 V395 " +
        "M215,256 H492 M437,188 L489,240 Q502,256 489,272 L437,324"

// Door with an arrow going in, the mirror of the one going out
private const val SignInPath =
    "M172,120 V100 A80,80 0 0 1 252,20 H412 A80,80 0 0 1 492,100 V412 A80,80 0 0 1 412,492 H252 A80,80 0 0 1 172,412 V395 " +
        "M20,256 H295 M240,188 L292,240 Q305,256 292,272 L240,324"
private const val LineIconGrid = 512f
private const val LineIconStroke = 40f

// Bulleted list
private const val LibraryPath =
    "M3,13h2v-2L3,11v2zM3,17h2v-2L3,15v2zM3,9h2L5,7L3,7v2zM7,13h14v-2L7,11v2zM7,17h14v-2L7,15v2zM7,7v2h14L21,7L7,7z"

// Material icons are drawn on a 24 unit grid and shown at 24dp
private const val IconGrid = 24f
private val IconSize = 24.dp

// Builds an icon from path data
private fun icon(name: String, pathData: String, grid: Float = IconGrid): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconSize,
        defaultHeight = IconSize,
        viewportWidth = grid,
        viewportHeight = grid
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        // Icons are tinted by the caller, so the fill color does not matter
        fill = SolidColor(Color.Black)
    ).build()

// Builds an icon drawn with a rounded line instead of a fill
private fun lineIcon(name: String, pathData: String, grid: Float, strokeWidth: Float): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconSize,
        defaultHeight = IconSize,
        viewportWidth = grid,
        viewportHeight = grid
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        // Icons are tinted by the caller, so the line color does not matter
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()

// Icons drawn in code so no icon library has to ship with the app
object WavvyIcons {
    val Home: ImageVector by lazy { icon("Home", HomePath) }
    val Explore: ImageVector by lazy { icon("Explore", ExplorePath) }
    val Discover: ImageVector by lazy { icon("Discover", DiscoverPath) }
    val Library: ImageVector by lazy { icon("Library", LibraryPath) }
    val Person: ImageVector by lazy { icon("Person", PersonPath) }
    val Bell: ImageVector by lazy { icon("Bell", BellPath) }
    val Back: ImageVector by lazy { icon("Back", BackPath) }
    val Login: ImageVector by lazy { icon("Login", LoginPath, LoginGrid) }
    val Integrations: ImageVector by lazy { icon("Integrations", IntegrationsPath) }
    val Settings: ImageVector by lazy { icon("Settings", SettingsPath) }
    val SignOut: ImageVector by lazy { lineIcon("SignOut", SignOutPath, LineIconGrid, LineIconStroke) }
    val SignIn: ImageVector by lazy { lineIcon("SignIn", SignInPath, LineIconGrid, LineIconStroke) }
}
