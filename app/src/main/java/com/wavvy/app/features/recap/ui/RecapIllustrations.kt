package com.wavvy.app.features.recap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The kinds of listener a year can say, each one has its own flat drawing
internal enum class Listener { Replayer, Specialist, Adventurer, Streaker, Nostalgic, Trendy, Collector, Nocturnal, EarlyBird, Visitor, Eclectic }

// The flat drawing of a kind of listener, made of plain shapes in the colors of the page and the ink of the card
@Composable
internal fun ListenerArt(kind: Listener, modifier: Modifier = Modifier) {
    val theme = LocalRecapTheme.current

    Canvas(modifier = modifier) {
        when (kind) {
            Listener.Replayer -> drawReplayer(theme.accent, theme.accentAlt)
            Listener.Specialist -> drawSpecialist(theme.accent, theme.accentAlt)
            Listener.Adventurer -> drawAdventurer(theme.accent, theme.accentAlt)
            Listener.Streaker -> drawStreaker(theme.accent, theme.accentAlt)
            Listener.Nostalgic -> drawNostalgic(theme.accent, theme.accentAlt)
            Listener.Trendy -> drawTrendy(theme.accent, theme.accentAlt)
            Listener.Collector -> drawCollector(theme.accent, theme.accentAlt)
            Listener.Nocturnal -> drawNocturnal(theme.accent, theme.accentAlt)
            Listener.EarlyBird -> drawEarlyBird(theme.accent, theme.accentAlt)
            Listener.Visitor -> drawVisitor(theme.accent, theme.accentAlt)
            Listener.Eclectic -> drawEclectic(theme.accent, theme.accentAlt)
        }
    }
}

// How thick the lines of the drawings are, how thin and how clear the grooves of the record are and how much the mixed circles show through
private const val LineWidth = 0.05f
private const val GrooveWidth = 0.012f
private const val GrooveAlpha = 0.25f
private const val MixAlpha = 0.85f

// A point and a length of the drawing, both as a part of the square it is drawn in
private fun DrawScope.at(x: Float, y: Float) = Offset(size.width * x, size.height * y)
private fun DrawScope.len(part: Float) = size.minDimension * part

// A closed shape drawn from points that are a part of the square
private fun DrawScope.shape(color: Color, build: Path.(w: Float, h: Float) -> Unit) {
    drawPath(Path().apply { build(size.width, size.height) }, color)
}

// A star with four points
private fun DrawScope.sparkle(color: Color, x: Float, y: Float, radius: Float) {
    val middle = at(x, y)
    val reach = len(radius)
    val path = Path().apply {
        moveTo(middle.x, middle.y - reach)
        quadraticTo(middle.x, middle.y, middle.x + reach, middle.y)
        quadraticTo(middle.x, middle.y, middle.x, middle.y + reach)
        quadraticTo(middle.x, middle.y, middle.x - reach, middle.y)
        quadraticTo(middle.x, middle.y, middle.x, middle.y - reach)
        close()
    }
    drawPath(path, color)
}

// A record that goes round, with the arrow of the loop around it
private fun DrawScope.drawReplayer(main: Color, second: Color) {
    val ink = RecapColors.CardInk
    drawCircle(ink, len(0.3f), at(0.5f, 0.5f))
    listOf(0.24f, 0.18f).forEach { drawCircle(Color.White.copy(alpha = GrooveAlpha), len(it), at(0.5f, 0.5f), style = Stroke(width = len(GrooveWidth))) }
    drawCircle(main, len(0.11f), at(0.5f, 0.5f))
    drawCircle(RecapColors.CardFace, len(0.025f), at(0.5f, 0.5f))

    // The arrow of the loop, an arc with its head at the end
    val start = -60f
    val sweep = 250f
    val radius = len(0.43f)
    val middle = at(0.5f, 0.5f)
    drawArc(second, start, sweep, false, Offset(middle.x - radius, middle.y - radius), Size(radius * 2f, radius * 2f), style = Stroke(width = len(LineWidth), cap = StrokeCap.Round))
    val angle = (start + sweep) * PI.toFloat() / 180f
    val end = Offset(middle.x + radius * cos(angle), middle.y + radius * sin(angle))
    val tangent = Offset(-sin(angle), cos(angle))
    val normal = Offset(cos(angle), sin(angle))
    val head = Path().apply {
        moveTo(end.x + tangent.x * len(0.09f), end.y + tangent.y * len(0.09f))
        lineTo(end.x + normal.x * len(0.06f), end.y + normal.y * len(0.06f))
        lineTo(end.x - normal.x * len(0.06f), end.y - normal.y * len(0.06f))
        close()
    }
    drawPath(head, second)
}

// A smiling star on its step, the favorite on top
private fun DrawScope.drawSpecialist(main: Color, second: Color) {
    val ink = RecapColors.CardInk
    drawRoundRect(second, at(0.22f, 0.7f), Size(len(0.56f), len(0.2f)), CornerRadius(len(0.04f)))

    val middle = at(0.5f, 0.42f)
    val outer = len(0.3f)
    val inner = len(0.13f)
    val star = Path().apply {
        for (point in 0 until 10) {
            val radius = if (point % 2 == 0) outer else inner
            val angle = (-90f + point * 36f) * PI.toFloat() / 180f
            val x = middle.x + radius * cos(angle)
            val y = middle.y + radius * sin(angle)
            if (point == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, main)
    drawCircle(ink, len(0.022f), at(0.43f, 0.43f))
    drawCircle(ink, len(0.022f), at(0.57f, 0.43f))
    drawArc(ink, 20f, 140f, false, at(0.43f, 0.43f), Size(len(0.14f), len(0.1f)), style = Stroke(width = len(0.02f), cap = StrokeCap.Round))
    sparkle(second, 0.17f, 0.22f, 0.07f)
    sparkle(second, 0.84f, 0.3f, 0.05f)
}

// A compass with its needle, for who always looks for the next find
private fun DrawScope.drawAdventurer(main: Color, second: Color) {
    val ink = RecapColors.CardInk
    drawCircle(second, len(0.42f), at(0.5f, 0.5f))
    drawCircle(RecapColors.CardFace, len(0.33f), at(0.5f, 0.5f))
    listOf(0f, 90f, 180f, 270f).forEach { turn ->
        rotate(turn, at(0.5f, 0.5f)) { drawLine(ink, at(0.5f, 0.2f), at(0.5f, 0.26f), len(0.022f), StrokeCap.Round) }
    }
    rotate(40f, at(0.5f, 0.5f)) {
        shape(main) { w, h -> moveTo(0.5f * w, 0.2f * h); lineTo(0.42f * w, 0.5f * h); lineTo(0.58f * w, 0.5f * h); close() }
        shape(ink) { w, h -> moveTo(0.5f * w, 0.8f * h); lineTo(0.42f * w, 0.5f * h); lineTo(0.58f * w, 0.5f * h); close() }
    }
    drawCircle(RecapColors.CardFace, len(0.03f), at(0.5f, 0.5f))
}

// A flame, for the days that came one after the other
private fun DrawScope.drawStreaker(main: Color, second: Color) {
    shape(main) { w, h ->
        moveTo(0.5f * w, 0.08f * h)
        cubicTo(0.52f * w, 0.28f * h, 0.8f * w, 0.38f * h, 0.8f * w, 0.62f * h)
        cubicTo(0.8f * w, 0.82f * h, 0.66f * w, 0.92f * h, 0.5f * w, 0.92f * h)
        cubicTo(0.34f * w, 0.92f * h, 0.2f * w, 0.82f * h, 0.2f * w, 0.62f * h)
        cubicTo(0.2f * w, 0.5f * h, 0.28f * w, 0.42f * h, 0.34f * w, 0.36f * h)
        cubicTo(0.36f * w, 0.46f * h, 0.42f * w, 0.5f * h, 0.46f * w, 0.48f * h)
        cubicTo(0.42f * w, 0.34f * h, 0.44f * w, 0.2f * h, 0.5f * w, 0.08f * h)
        close()
    }
    shape(second) { w, h ->
        moveTo(0.5f * w, 0.46f * h)
        cubicTo(0.52f * w, 0.56f * h, 0.64f * w, 0.6f * h, 0.64f * w, 0.72f * h)
        cubicTo(0.64f * w, 0.84f * h, 0.57f * w, 0.9f * h, 0.5f * w, 0.9f * h)
        cubicTo(0.43f * w, 0.9f * h, 0.36f * w, 0.84f * h, 0.36f * w, 0.72f * h)
        cubicTo(0.36f * w, 0.62f * h, 0.46f * w, 0.58f * h, 0.5f * w, 0.46f * h)
        close()
    }
}

// A cassette tape, for who keeps the old songs close
private fun DrawScope.drawNostalgic(main: Color, second: Color) {
    val ink = RecapColors.CardInk
    drawRoundRect(ink, at(0.08f, 0.26f), Size(len(0.84f), len(0.5f)), CornerRadius(len(0.06f)))
    drawRoundRect(second, at(0.16f, 0.32f), Size(len(0.68f), len(0.24f)), CornerRadius(len(0.03f)))
    listOf(0.34f, 0.66f).forEach { x ->
        drawCircle(RecapColors.CardFace, len(0.075f), at(x, 0.44f))
        drawCircle(main, len(0.03f), at(x, 0.44f))
    }
    drawRoundRect(main, at(0.3f, 0.6f), Size(len(0.4f), len(0.12f)), CornerRadius(len(0.03f)))
    listOf(0.4f, 0.5f, 0.6f).forEach { x -> drawCircle(ink, len(0.014f), at(x, 0.66f)) }
}

// A rocket taking off, for who is always with the newest
private fun DrawScope.drawTrendy(main: Color, second: Color) {
    val ink = RecapColors.CardInk
    rotate(30f, at(0.5f, 0.5f)) {
        shape(second) { w, h -> moveTo(0.34f * w, 0.52f * h); lineTo(0.18f * w, 0.76f * h); lineTo(0.36f * w, 0.7f * h); close() }
        shape(second) { w, h -> moveTo(0.66f * w, 0.52f * h); lineTo(0.82f * w, 0.76f * h); lineTo(0.64f * w, 0.7f * h); close() }
        shape(second) { w, h -> moveTo(0.43f * w, 0.78f * h); lineTo(0.5f * w, 0.96f * h); lineTo(0.57f * w, 0.78f * h); close() }
        shape(main) { w, h ->
            moveTo(0.5f * w, 0.06f * h)
            cubicTo(0.74f * w, 0.2f * h, 0.74f * w, 0.55f * h, 0.66f * w, 0.72f * h)
            lineTo(0.34f * w, 0.72f * h)
            cubicTo(0.26f * w, 0.55f * h, 0.26f * w, 0.2f * h, 0.5f * w, 0.06f * h)
            close()
        }
        drawRoundRect(ink, at(0.42f, 0.72f), Size(len(0.16f), len(0.06f)), CornerRadius(len(0.02f)))
        drawCircle(ink, len(0.1f), at(0.5f, 0.36f))
        drawCircle(RecapColors.CardFace, len(0.066f), at(0.5f, 0.36f))
    }
}

// Albums piled one over the other, for who keeps the whole record
private fun DrawScope.drawCollector(main: Color, second: Color) {
    val ink = RecapColors.CardInk
    val side = Size(len(0.46f), len(0.46f))
    val corner = CornerRadius(len(0.05f))

    rotate(-12f, at(0.36f, 0.42f)) { drawRoundRect(ink, at(0.36f - 0.23f, 0.42f - 0.23f), side, corner) }
    rotate(10f, at(0.64f, 0.4f)) { drawRoundRect(second, at(0.64f - 0.23f, 0.4f - 0.23f), side, corner) }
    drawRoundRect(main, at(0.5f - 0.23f, 0.62f - 0.23f), side, corner)
    drawCircle(RecapColors.CardFace, len(0.14f), at(0.5f, 0.62f))
    drawCircle(ink, len(0.05f), at(0.5f, 0.62f))
}

// A crescent moon with stars, for who listens when the city sleeps
private fun DrawScope.drawNocturnal(main: Color, second: Color) {
    drawCircle(main, len(0.32f), at(0.46f, 0.5f))
    drawCircle(RecapColors.CardFace, len(0.27f), at(0.62f, 0.42f))
    sparkle(second, 0.76f, 0.24f, 0.09f)
    sparkle(second, 0.82f, 0.58f, 0.06f)
    sparkle(second, 0.64f, 0.8f, 0.045f)
}

// A sun coming up over the horizon, for who starts the day with a song
private fun DrawScope.drawEarlyBird(main: Color, second: Color) {
    val middle = at(0.5f, 0.7f)
    val radius = len(0.26f)
    drawArc(main, 180f, 180f, true, Offset(middle.x - radius, middle.y - radius), Size(radius * 2f, radius * 2f))
    for (step in 0 until 5) {
        val angle = (200f + step * 35f) * PI.toFloat() / 180f
        val from = Offset(middle.x + len(0.34f) * cos(angle), middle.y + len(0.34f) * sin(angle))
        val to = Offset(middle.x + len(0.44f) * cos(angle), middle.y + len(0.44f) * sin(angle))
        drawLine(second, from, to, len(LineWidth), StrokeCap.Round)
    }
    drawLine(RecapColors.CardInk, at(0.1f, 0.74f), at(0.9f, 0.74f), len(LineWidth), StrokeCap.Round)
}

// A door left open with the light coming through, for who has just arrived
private fun DrawScope.drawVisitor(main: Color, second: Color) {
    drawRoundRect(RecapColors.CardInk, at(0.22f, 0.1f), Size(len(0.56f), len(0.8f)), CornerRadius(len(0.04f)))
    drawRect(second, at(0.27f, 0.15f), Size(len(0.46f), len(0.75f)))
    shape(main) { w, h -> moveTo(0.27f * w, 0.15f * h); lineTo(0.5f * w, 0.24f * h); lineTo(0.5f * w, 0.84f * h); lineTo(0.27f * w, 0.9f * h); close() }
    drawCircle(RecapColors.CardFace, len(0.02f), at(0.45f, 0.55f))
    sparkle(second, 0.86f, 0.2f, 0.07f)
    sparkle(second, 0.14f, 0.7f, 0.05f)
}

// Three circles that mix, for who goes wherever the day takes them
private fun DrawScope.drawEclectic(main: Color, second: Color) {
    drawCircle(main.copy(alpha = MixAlpha), len(0.26f), at(0.38f, 0.4f))
    drawCircle(second.copy(alpha = MixAlpha), len(0.26f), at(0.62f, 0.4f))
    drawCircle(RecapColors.CardInk.copy(alpha = MixAlpha), len(0.26f), at(0.5f, 0.62f))
}
