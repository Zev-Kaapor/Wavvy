package com.lonewolf.wavvy.ui.home.components

// UI and layout mechanics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.SectionTitle
import com.lonewolf.wavvy.ui.theme.GenreGradients
import com.lonewolf.wavvy.ui.theme.Poppins

// Genre data model
data class Genre(val nameResId: Int, val gradient: Brush)

// Scrollable grid for music genres
@Composable
fun GenreSection(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = listOf(
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

    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_genres))

        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(220.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(genres) { genre ->
                GenreCard(
                    genre = genre,
                    onClick = { onItemClick(it) }
                )
            }
        }
    }
}

// Individual genre card with gradient background - Refined for AMOLED
@Composable
fun GenreCard(
    genre: Genre,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genreName = stringResource(genre.nameResId)

    Box(
        modifier = modifier
            .width(150.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(genre.gradient)
            .clickable { onClick(genreName) }
    ) {
        // Subtle dark scrim to ensure text pops regardless of gradient brightness
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )

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
