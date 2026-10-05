package com.wavvy.app.core.designsystem.icons

// UI graphics and vectors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
}
