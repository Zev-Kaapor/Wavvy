package com.wavvy.app.features.recap.ui

// Animation
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
// Math
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// One whole turn in degrees and in radians, and where the arches start and how far they go, from the top to the end side
private const val FullTurn = 360f
private const val TwoPi = 2f * PI.toFloat()
private const val ArchStart = -90f
private const val ArchSweep = 90f

// A number that goes from zero to one over and over, every lap taking the time it is given, which the shapes read only while drawing so they move without building the page again
@Composable
private fun rememberLoop(millis: Int, label: String): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(millis, easing = LinearEasing), RepeatMode.Restart),
        label = label
    )

// Where the points of the crown are and where its beads sit, as shares of its width and height, and the size of the beads and of the soft corners
private val CrownOutline = listOf(0.1f to 0.86f, 0.04f to 0.3f, 0.28f to 0.56f, 0.5f to 0.16f, 0.72f to 0.56f, 0.96f to 0.3f, 0.9f to 0.86f)
private val CrownBeads = listOf(0.04f to 0.3f, 0.5f to 0.16f, 0.96f to 0.3f)
private const val CrownBead = 0.07f
private const val CrownSoft = 0.025f
private const val CrownBandTop = 0.86f
private const val CrownBandRound = 0.05f

// A crown with three points ending in round beads, over a band, leaning to the left as if worn askew
// It is drawn inside a room of its own around it, so the lean never takes a corner out of the box and the box never cuts it
@Composable
fun Crown(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(RecapDimens.CrownRoom)) {
        rotate(-RecapDimens.CrownTilt, pivot = center) {
            val body = Path().apply {
                CrownOutline.forEachIndexed { index, (x, y) ->
                    if (index == 0) moveTo(size.width * x, size.height * y) else lineTo(size.width * x, size.height * y)
                }
                close()
            }
            drawPath(body, color)
            drawPath(body, color, style = Stroke(width = size.width * CrownSoft, join = StrokeJoin.Round))
            CrownBeads.forEach { (x, y) -> drawCircle(color, size.width * CrownBead, Offset(size.width * x, size.height * y)) }
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * CrownOutline.first().first - size.width * CrownSoft, size.height * CrownBandTop),
                size = Size(size.width * (CrownOutline.last().first - CrownOutline.first().first) + 2 * size.width * CrownSoft, size.height * (1f - CrownBandTop)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * CrownBandRound)
            )
        }
    }
}

// A regular shape with the number of sides it is given, as the diamond with four, the heptagon with seven, the nonagon with nine and the decagon with ten
// It stands with a point up, turns slowly to one side or the other, and is solid or only a line
@Composable
fun Polygon(color: Color, sides: Int, modifier: Modifier = Modifier, isOutline: Boolean = false, direction: Float = 1f) {
    val loop = rememberLoop(RecapDimens.SlowLapMillis, "Polygon")

    Canvas(modifier = modifier.graphicsLayer { rotationZ = direction * loop.value * FullTurn }) {
        val stroke = RecapDimens.PolygonStroke.toPx()
        // The line is kept inside the room of the shape, so its points are never cut
        val radius = size.minDimension / 2f - if (isOutline) stroke else 0f
        val path = Path()
        for (index in 0 until sides) {
            val angle = -PI.toFloat() / 2f + index * TwoPi / sides
            val point = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        if (isOutline) drawPath(path, color, style = Stroke(width = stroke, join = StrokeJoin.Round)) else drawPath(path, color)
    }
}

// A star with many points, the shape that frames a cover, it turns slowly when asked to
@Composable
fun Starburst(color: Color, modifier: Modifier = Modifier, isSpinning: Boolean = true) {
    val loop = rememberLoop(RecapDimens.StarLapMillis, "Starburst")

    Canvas(modifier = modifier.graphicsLayer { rotationZ = if (isSpinning) loop.value * FullTurn else 0f }) {
        val outer = size.minDimension / 2f
        val path = Path()
        val steps = RecapDimens.StarPoints * 2
        for (step in 0 until steps) {
            val radius = if (step % 2 == 0) outer else outer * RecapDimens.StarInner
            val angle = step * PI.toFloat() / RecapDimens.StarPoints
            val point = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
            if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color)
    }
}

// A flower of round petals around a round center, turning slowly
@Composable
fun Flower(color: Color, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.SlowLapMillis, "Flower")

    Canvas(modifier = modifier.graphicsLayer { rotationZ = loop.value * FullTurn }) {
        val petal = size.minDimension * RecapDimens.PetalSize
        val reach = size.minDimension / 2f - petal
        for (index in 0 until RecapDimens.PetalCount) {
            val angle = index * TwoPi / RecapDimens.PetalCount
            drawCircle(color, petal, Offset(center.x + reach * cos(angle), center.y + reach * sin(angle)))
        }
        drawCircle(color, reach, center)
    }
}

// A round shape with a wavy edge, never the same circle twice because of its seed, turning slowly
@Composable
fun Blob(color: Color, seed: Int, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.SlowLapMillis, "Blob")

    Canvas(modifier = modifier.graphicsLayer { rotationZ = -loop.value * FullTurn }) {
        val base = size.minDimension / 2f * RecapDimens.BlobBase
        val path = Path()
        for (step in 0..RecapDimens.BlobPoints) {
            val angle = step * TwoPi / RecapDimens.BlobPoints
            val radius = base * (1f + RecapDimens.BlobWobble * sin(RecapDimens.BlobLobes * angle + seed) + RecapDimens.BlobWobbleAlt * sin((RecapDimens.BlobLobes + 2) * angle + 2 * seed))
            val point = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
            if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color)
    }
}

// A round shape that goes from one color to another, with no edge to speak of, breathing in and out
@Composable
fun Orb(inner: Color, outer: Color, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.PulseMillis, "Orb")

    Canvas(
        modifier = modifier.graphicsLayer {
            val scale = 1f + RecapDimens.PulseDepth * sin(loop.value * TwoPi)
            scaleX = scale
            scaleY = scale
        }
    ) {
        drawCircle(Brush.radialGradient(listOf(inner, outer), center = center, radius = size.minDimension / 2f), size.minDimension / 2f, center)
    }
}

// Circles one inside the other, only their lines, breathing in and out
@Composable
fun Rings(color: Color, thickness: Dp, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.PulseMillis, "Rings")

    Canvas(
        modifier = modifier.graphicsLayer {
            val scale = 1f + RecapDimens.PulseDepth * sin(loop.value * TwoPi)
            scaleX = scale
            scaleY = scale
        }
    ) {
        val outer = size.minDimension / 2f - thickness.toPx()
        for (index in 1..RecapDimens.RingCount) {
            drawCircle(color, outer * index / RecapDimens.RingCount, center, style = Stroke(width = thickness.toPx()))
        }
    }
}

// Round dots in rows that grow from one corner to the other, as in a printed picture, a swell going through them, mirrored they grow from the other side
@Composable
fun Dots(color: Color, cell: Dp, modifier: Modifier = Modifier, isMirrored: Boolean = false) {
    val loop = rememberLoop(RecapDimens.ShapeLoopMillis, "Dots")

    Canvas(modifier = modifier) {
        val side = cell.toPx()
        val columns = (size.width / side).toInt() + 1
        val rows = (size.height / side).toInt() + 1
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val across = if (isMirrored) columns - 1 - column else column
                val grow = (row + across + 2f) / (rows + columns + 2f)
                val swell = 1f + RecapDimens.DotSwell * sin(loop.value * TwoPi - (row + across) * RecapDimens.DotSwellStep)
                drawCircle(color, side * RecapDimens.DotMax * grow * swell, Offset(column * side + side / 2f, row * side + side / 2f))
            }
        }
    }
}

// Stars with four points, a few of them scattered, each one lighting up and fading by turns
@Composable
fun Sparkles(color: Color, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.ShapeLoopMillis, "Sparkles")

    Canvas(modifier = modifier) {
        RecapDimens.SparklePlaces.forEachIndexed { index, place ->
            val radius = size.minDimension * RecapDimens.SparkleSize * (1f - index * RecapDimens.SparkleShrink)
            val middle = Offset(size.width * place.x, size.height * place.y)
            val light = RecapDimens.SparkleLeast + (1f - RecapDimens.SparkleLeast) * (0.5f + 0.5f * sin(loop.value * TwoPi + index * RecapDimens.SparkleOffset))
            val path = Path().apply {
                moveTo(middle.x, middle.y - radius)
                quadraticTo(middle.x, middle.y, middle.x + radius, middle.y)
                quadraticTo(middle.x, middle.y, middle.x, middle.y + radius)
                quadraticTo(middle.x, middle.y, middle.x - radius, middle.y)
                quadraticTo(middle.x, middle.y, middle.x, middle.y - radius)
                close()
            }
            drawPath(path, color.copy(alpha = color.alpha * light))
        }
    }
}

// Thick lines that roll up and down like water, the water moving along
@Composable
fun Wave(color: Color, thickness: Dp, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.ShapeLoopMillis, "Wave")

    Canvas(modifier = modifier) {
        val gap = size.height / (RecapDimens.WaveCount + 1)
        for (line in 1..RecapDimens.WaveCount) {
            val path = Path()
            for (step in 0..RecapDimens.WaveSteps) {
                val x = size.width * step / RecapDimens.WaveSteps
                val y = gap * line + size.height * RecapDimens.WaveAmplitude * sin(step * TwoPi / RecapDimens.WaveSteps * RecapDimens.WaveTurns + line - loop.value * TwoPi)
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// Squares of two colors one beside the other, as a chessboard, going slowly to the side
@Composable
fun Checkerboard(first: Color, second: Color, cell: Dp, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.SlideMillis, "Checkerboard")

    Canvas(modifier = modifier) {
        val side = cell.toPx()
        val columns = (size.width / side).toInt() + 2
        val rows = (size.height / side).toInt() + 1
        // Two squares are one repeat of the pattern, so going one repeat per lap never shows a jump
        val shift = loop.value * side * 2f
        for (row in 0 until rows) {
            for (column in -2..columns) {
                drawRect(if (Math.floorMod(row + column, 2) == 0) first else second, Offset(column * side + shift, row * side), Size(side, side))
            }
        }
    }
}

// Rhombuses side by side, in two colors, going slowly to the side
@Composable
fun Diamonds(first: Color, second: Color, cell: Dp, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.SlideMillis, "Diamonds")

    Canvas(modifier = modifier) {
        val side = cell.toPx()
        val columns = (size.width / side).toInt() + 2
        val rows = (size.height / side).toInt() + 1
        val shift = loop.value * side * 2f
        for (row in 0 until rows) {
            for (column in -2..columns) {
                val middle = Offset(column * side + shift + if (row % 2 == 0) 0f else side / 2f, row * side + side / 2f)
                val path = Path().apply {
                    moveTo(middle.x, middle.y - side / 2f)
                    lineTo(middle.x + side / 2f, middle.y)
                    lineTo(middle.x, middle.y + side / 2f)
                    lineTo(middle.x - side / 2f, middle.y)
                    close()
                }
                drawPath(path, if (Math.floorMod(row + column, 2) == 0) first else second)
            }
        }
    }
}

// Steps going up from one corner, square as pixels
@Composable
fun Stairs(color: Color, step: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = step.toPx()
        val count = (size.width / side).toInt() + 1
        val levels = (size.height / side).toInt()
        for (index in 0 until count) {
            val height = side * (index + 1).coerceAtMost(levels)
            drawRect(color, Offset(size.width - side * (index + 1), size.height - height), Size(side, height))
        }
    }
}

// Quarter circles one inside the other that grow from the corner of the shape, as a rainbow, only their lines, breathing in and out
@Composable
fun Arches(color: Color, thickness: Dp, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.PulseMillis, "Arches")

    Canvas(modifier = modifier) {
        val origin = Offset(0f, size.height)
        for (index in 1..RecapDimens.ArchCount) {
            val breath = 1f + RecapDimens.PulseDepth * sin(loop.value * TwoPi - index * RecapDimens.ArchStep)
            val radius = (size.minDimension * index / RecapDimens.ArchCount - thickness.toPx()) * breath
            drawArc(
                color = color,
                startAngle = ArchStart,
                sweepAngle = ArchSweep,
                useCenter = false,
                topLeft = Offset(origin.x - radius, origin.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = thickness.toPx())
            )
        }
    }
}

// A thick line that goes up and down with sharp corners, rocking gently
@Composable
fun Zigzag(color: Color, thickness: Dp, modifier: Modifier = Modifier) {
    val loop = rememberLoop(RecapDimens.ShapeLoopMillis, "Zigzag")

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = RecapDimens.ZigzagBob.toPx() * sin(loop.value * TwoPi)
        }
    ) {
        val step = size.width / RecapDimens.ZigzagSteps
        val path = Path().apply {
            moveTo(0f, size.height)
            for (index in 1..RecapDimens.ZigzagSteps) lineTo(index * step, if (index % 2 == 0) size.height else 0f)
        }
        drawPath(path, color, style = Stroke(width = thickness.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter))
    }
}

// Puts a shape in a corner of the page and pushes it partly out of the screen, by a share of its own size
private fun BoxScope.corner(alignment: Alignment, size: Dp, outX: Float, outY: Float): Modifier =
    Modifier
        .align(alignment)
        .size(size)
        .graphicsLayer { translationX = this.size.width * outX; translationY = this.size.height * outY }

// The shapes behind the words of a page, each arrangement uses other shapes in other corners, so no two pages that follow look the same
@Composable
fun BoxScope.Decor(variant: Int) {
    val theme = LocalRecapTheme.current
    val big = RecapDimens.DecorBig
    val small = RecapDimens.DecorSmall
    val out = RecapDimens.DecorOut
    val wide = RecapDimens.DecorWide
    val rise = RecapDimens.DecorRise

    // A calm arrangement with a small shape in the top corner and a little strip low in the page, for the pages whose words go all the way down
    if (variant == RecapDimens.DecorCalm) {
        Polygon(theme.accent, RecapDimens.PolygonCornerSides, corner(Alignment.TopEnd, small, out, -rise))
        Row(
            horizontalArrangement = Arrangement.spacedBy(RecapDimens.PolygonStripGap),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = RecapDimens.PolygonStripBottom)
        ) {
            Polygon(theme.accentAlt, 4, Modifier.size(RecapDimens.PolygonStrip))
            Polygon(theme.accentAlt, 10, Modifier.size(RecapDimens.PolygonStrip), isOutline = true, direction = -1f)
            Polygon(theme.accentAlt, 9, Modifier.size(RecapDimens.PolygonStrip))
        }
        return
    }

    // The arrangement of the page of the minutes, the blob low in the page and the sparkles high in the corner, away from the number
    if (variant == RecapDimens.DecorMinutes) {
        Blob(theme.accent, 3, corner(Alignment.BottomStart, big, -out, out))
        Sparkles(theme.accentAlt, Modifier.align(Alignment.TopEnd).padding(top = RecapDimens.DotsHigh).size(RecapDimens.SparklesBox))
        return
    }

    // The arrangement of the page of the missed releases, small sparkles high in the corner and small rings low on the end side, pushed out to the edge, away from the words and the list
    if (variant == RecapDimens.DecorMissed) {
        Sparkles(theme.accentAlt, Modifier.align(Alignment.TopStart).padding(top = RecapDimens.DotsHigh).size(RecapDimens.SparklesSmall))
        Rings(theme.accent, RecapDimens.RingStroke, corner(Alignment.BottomEnd, small, RecapDimens.RingsOut, RecapDimens.RingsOut))
        return
    }

    // The arrangement of the list of ten songs, a single small flower low in the corner, so it does not cover the name of a long song, nothing over the title
    if (variant == RecapDimens.DecorTopSongs) {
        Flower(theme.accent, corner(Alignment.BottomEnd, small, out, out))
        return
    }

    // The arrangement of the page of the songs played, a blob low in the page and dots high, with no small stars over the words
    if (variant == RecapDimens.DecorSongsTease) {
        Blob(theme.accent, 3, corner(Alignment.BottomStart, big, -out, out))
        Dots(theme.accentAlt, RecapDimens.DotCell, Modifier.align(Alignment.TopEnd).padding(top = RecapDimens.DotsHigh).size(RecapDimens.DotsWidth, RecapDimens.DotsHeight), isMirrored = true)
        return
    }

    // The arrangement of the page of the most played song, with the shapes low in the page, away from the words and from the cover with its star
    if (variant == RecapDimens.DecorSong) {
        Blob(theme.accent, 3, corner(Alignment.BottomStart, big, -out, RecapDimens.DecorSink))
        Dots(theme.accentAlt, RecapDimens.DotCell, Modifier.align(Alignment.BottomEnd).size(RecapDimens.DotsWidth, RecapDimens.DotsHeight))
        return
    }

    when (variant % RecapDimens.DecorVariants) {
        0 -> {
            Starburst(theme.accent, corner(Alignment.TopEnd, big, out, -rise))
            Zigzag(theme.accentAlt, RecapDimens.ZigzagThickness, Modifier.align(Alignment.BottomStart).size(RecapDimens.ZigzagWidth, RecapDimens.ZigzagHeight))
        }
        1 -> {
            Flower(theme.accent, corner(Alignment.BottomEnd, big, out, out))
            Wave(theme.accentAlt, RecapDimens.WaveThickness, Modifier.align(Alignment.TopStart).padding(top = RecapDimens.WaveTop).size(RecapDimens.WaveWidth, RecapDimens.WaveHeight))
        }
        2 -> {
            Checkerboard(theme.accent, theme.accentAlt, RecapDimens.CheckerCell, Modifier.align(Alignment.BottomCenter).size(RecapDimens.CheckerWidth, RecapDimens.CheckerHeight))
            Starburst(theme.accentAlt, corner(Alignment.TopStart, small, -out, -rise))
        }
        3 -> {
            Orb(theme.accentAlt, theme.accent, corner(Alignment.TopEnd, big, out, -rise))
            Dots(theme.accent, RecapDimens.DotCell, Modifier.align(Alignment.BottomStart).size(RecapDimens.DotsWidth, RecapDimens.DotsHeight), isMirrored = true)
        }
        4 -> {
            Rings(theme.accent, RecapDimens.RingStroke, corner(Alignment.BottomEnd, big, out, out))
            Sparkles(theme.accentAlt, Modifier.align(Alignment.TopStart).padding(top = RecapDimens.DecorTop).size(RecapDimens.SparklesBox))
        }
        5 -> {
            Arches(theme.accent, RecapDimens.ArchStroke, Modifier.align(Alignment.BottomStart).size(small))
            Blob(theme.accentAlt, 1, corner(Alignment.TopEnd, big, out, -rise))
        }
        6 -> {
            Diamonds(theme.accent, theme.accentAlt, RecapDimens.DiamondCell, Modifier.align(Alignment.BottomCenter).size(RecapDimens.CheckerWidth, RecapDimens.DiamondHeight))
            Flower(theme.accentAlt, corner(Alignment.TopStart, small, -out, -rise))
        }
        7 -> {
            Stairs(theme.accent, RecapDimens.StairStep, Modifier.align(Alignment.BottomEnd).size(wide, RecapDimens.StairHeight))
            Orb(theme.accentAlt, theme.accent, corner(Alignment.TopStart, big, -out, -rise))
        }
        8 -> {
            Wave(theme.accent, RecapDimens.WaveThickness, Modifier.align(Alignment.BottomCenter).offset(y = RecapDimens.WaveSink).size(RecapDimens.CheckerWidth, RecapDimens.WaveHeight))
            Starburst(theme.accentAlt, corner(Alignment.TopEnd, small, out, -rise))
        }
        else -> {
            Blob(theme.accent, 3, corner(Alignment.BottomStart, big, -out, out))
            Dots(theme.accentAlt, RecapDimens.DotCell, Modifier.align(Alignment.TopEnd).padding(top = RecapDimens.DecorTop).size(RecapDimens.DotsWidth, RecapDimens.DotsHeight))
            Sparkles(theme.accent, Modifier.align(Alignment.CenterEnd).size(RecapDimens.SparklesBox))
        }
    }
}
