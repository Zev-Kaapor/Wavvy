package com.wavvy.app.features.discover.ui.components

// Compose layouts and foundations
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI utilities and graphics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project components and themes
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SectionTitle
import com.wavvy.app.core.designsystem.theme.GenreGradients
import com.wavvy.app.core.designsystem.theme.Poppins
import com.wavvy.app.core.designsystem.theme.onContentColor

// Genre domain model
data class Genre(val nameResId: Int, val gradient: Brush, val baseColor: Color)

private val genresList = listOf(
    Genre(R.string.genre_pop, GenreGradients.pop, Color(0xFF6200EE)),
    Genre(R.string.genre_rock, GenreGradients.rock, Color(0xFF0C0C12)),
    Genre(R.string.genre_hiphop, GenreGradients.hiphop, Color(0xFFD50000)),
    Genre(R.string.genre_electronic, GenreGradients.electronic, Color(0xFF311B92)),
    Genre(R.string.genre_indie, GenreGradients.indie, Color(0xFF00796B)),
    Genre(R.string.genre_lofi, GenreGradients.lofi, Color(0xFF311B92)),
    Genre(R.string.genre_jazz, GenreGradients.jazz, Color(0xFF1A237E)),
    Genre(R.string.genre_soul, GenreGradients.soul, Color(0xFF4E342E)),
    Genre(R.string.genre_rnb, GenreGradients.rnb, Color(0xFF26004D)),
    Genre(R.string.genre_ambient, GenreGradients.ambient, Color(0xFF006064)),
    Genre(R.string.genre_metal, GenreGradients.metal, Color(0xFF121214)),
    Genre(R.string.genre_punk, GenreGradients.punk, Color(0xFF6B001C)),
    Genre(R.string.genre_hardrock, GenreGradients.hardrock, Color(0xFF1A000A)),
    Genre(R.string.genre_phonk, GenreGradients.phonk, Color(0xFF0F0026)),
    Genre(R.string.genre_trap, GenreGradients.trap, Color(0xFF10171A)),
    Genre(R.string.genre_flamenco, GenreGradients.flamenco, Color(0xFF7F0000)),
    Genre(R.string.genre_arabic, GenreGradients.arabic, Color(0xFFE65100)),
    Genre(R.string.genre_greek, GenreGradients.greek, Color(0xFF0A47A1)),
    Genre(R.string.genre_kpop, GenreGradients.kpop, Color(0xFF651FFF)),
    Genre(R.string.genre_jpop, GenreGradients.jpop, Color(0xFFC2185B)),
    Genre(R.string.genre_cpop, GenreGradients.cpop, Color(0xFFFF9100)),
    Genre(R.string.genre_hindustani, GenreGradients.hindustani, Color(0xFF9C27B0)),
    Genre(R.string.genre_mpb, GenreGradients.mpb, Color(0xFF1B5E20)),
    Genre(R.string.genre_funk, GenreGradients.funk, Color(0xFF4A148C)),
    Genre(R.string.genre_sertanejo, GenreGradients.sertanejo, Color(0xFF4E342E)),
    Genre(R.string.genre_pagode, GenreGradients.pagode, Color(0xFFE65100)),
    Genre(R.string.genre_rap_nacional, GenreGradients.rapNacional, Color(0xFF263238)),
    Genre(R.string.genre_reggaeton, GenreGradients.reggaeton, Color(0xFF880E4F)),
    Genre(R.string.genre_afrobeat, GenreGradients.afrobeat, Color(0xFFD84315)),
    Genre(R.string.genre_reggae, GenreGradients.reggae, Color(0xFFF44336)),
    Genre(R.string.genre_vaporwave, GenreGradients.vaporwave, Color(0xFF00B2FF)),
    Genre(R.string.genre_synthwave, GenreGradients.synthwave, Color(0xFF00B2FF)),
    Genre(R.string.genre_citypop, GenreGradients.citypop, Color(0xFF0D47A1)),
    Genre(R.string.genre_darkwave, GenreGradients.darkwave, Color(0xFF000000)),
    Genre(R.string.genre_dreamcore, GenreGradients.dreamcore, Color(0xFF311B92)),
    Genre(R.string.genre_chillwave, GenreGradients.chillwave, Color(0xFF004D40))
)

// Genres section grid layout
@Composable
fun GenreSection(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columnCount = if (isLandscape) 4 else 2

    val chunkedGenres = remember(columnCount) { genresList.chunked(columnCount) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_genres))

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chunkedGenres.forEach { rowGenres ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowGenres.forEach { genre ->
                        GenreCard(
                            genre = genre,
                            onGenreClick = onItemClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowGenres.size < columnCount) {
                        repeat(columnCount - rowGenres.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// Genre card item layout
@Composable
fun GenreCard(
    genre: Genre,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genreName = stringResource(genre.nameResId)

    // Gradient scrim overlay
    val scrimGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
            startY = 0f
        )
    }

    // Adaptive text content color
    val textColor = remember(genre.baseColor) {
        lerp(genre.baseColor, Color.Black, 0.45f).onContentColor
    }

    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawWithCache {
                onDrawBehind {
                    drawRect(genre.gradient)
                    drawRect(scrimGradient)
                }
            }
            .clickable { onGenreClick(genreName) }
    ) {
        Text(
            text = genreName,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = textColor,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
