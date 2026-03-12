package com.lonewolf.wavvy.ui.home.components

// Compose layout and foundation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.sections.SectionTitle
import com.lonewolf.wavvy.ui.theme.GenreGradients
import com.lonewolf.wavvy.ui.theme.Poppins

// Genre data model
data class Genre(val nameResId: Int, val gradient: Brush)

// Static data source
private val genresList = listOf(
    Genre(R.string.genre_pop, GenreGradients.pop),
    Genre(R.string.genre_rock, GenreGradients.rock),
    Genre(R.string.genre_hiphop, GenreGradients.hiphop),
    Genre(R.string.genre_electronic, GenreGradients.electronic),
    Genre(R.string.genre_indie, GenreGradients.indie),
    Genre(R.string.genre_lofi, GenreGradients.lofi),
    Genre(R.string.genre_jazz, GenreGradients.jazz),
    Genre(R.string.genre_soul, GenreGradients.soul),
    Genre(R.string.genre_rnb, GenreGradients.rnb),
    Genre(R.string.genre_ambient, GenreGradients.ambient),
    Genre(R.string.genre_metal, GenreGradients.metal),
    Genre(R.string.genre_punk, GenreGradients.punk),
    Genre(R.string.genre_hardrock, GenreGradients.hardrock),
    Genre(R.string.genre_phonk, GenreGradients.phonk),
    Genre(R.string.genre_trap, GenreGradients.trap),
    Genre(R.string.genre_flamenco, GenreGradients.flamenco),
    Genre(R.string.genre_arabic, GenreGradients.arabic),
    Genre(R.string.genre_greek, GenreGradients.greek),
    Genre(R.string.genre_kpop, GenreGradients.kpop),
    Genre(R.string.genre_jpop, GenreGradients.jpop),
    Genre(R.string.genre_cpop, GenreGradients.cpop),
    Genre(R.string.genre_hindustani, GenreGradients.hindustani),
    Genre(R.string.genre_mpb, GenreGradients.mpb),
    Genre(R.string.genre_funk, GenreGradients.funk),
    Genre(R.string.genre_sertanejo, GenreGradients.sertanejo),
    Genre(R.string.genre_pagode, GenreGradients.pagode),
    Genre(R.string.genre_rap_nacional, GenreGradients.rapNacional),
    Genre(R.string.genre_reggaeton, GenreGradients.reggaeton),
    Genre(R.string.genre_afrobeat, GenreGradients.afrobeat),
    Genre(R.string.genre_reggae, GenreGradients.reggae),
    Genre(R.string.genre_vaporwave, GenreGradients.vaporwave),
    Genre(R.string.genre_synthwave, GenreGradients.synthwave),
    Genre(R.string.genre_citypop, GenreGradients.citypop),
    Genre(R.string.genre_darkwave, GenreGradients.darkwave),
    Genre(R.string.genre_dreamcore, GenreGradients.dreamcore),
    Genre(R.string.genre_chillwave, GenreGradients.chillwave)
)

// Main genre grid section
@Composable
fun GenreSection(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        SectionTitle(text = stringResource(R.string.section_title_genres))

        // Horizontal grid
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(220.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = genresList,
                key = { it.nameResId },
                contentType = { "genre_card" }
            ) { genre ->
                GenreCard(
                    genre = genre,
                    onGenreClick = onItemClick
                )
            }
        }
    }
}

// Individual genre card
@Composable
fun GenreCard(
    genre: Genre,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genreName = stringResource(genre.nameResId)
    val overlayColor = remember { Color.Black.copy(alpha = 0.15f) }

    Box(
        modifier = modifier
            .width(150.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawWithCache {
                onDrawBehind {
                    drawRect(genre.gradient)
                    drawRect(overlayColor)
                }
            }
            .clickable { onGenreClick(genreName) }
    ) {
        // Genre label
        Text(
            text = genreName,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
