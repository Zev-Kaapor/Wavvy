package com.lonewolf.wavvy.ui.search.results.components

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// UI utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.SearchCategory
import com.lonewolf.wavvy.ui.theme.Poppins

// Coordinator for search result skeleton lists
@Composable
fun SearchResultList(
    category: SearchCategory,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (category == SearchCategory.ALBUMS) {
            // Grid layout for albums
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(10) { 
                    AlbumGridItem(onClick = onItemClick) 
                }
            }
        } else {
            // List layout for other categories
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (category) {
                    SearchCategory.ALL -> {
                        // Top match
                        item { ResultHeader(stringResource(R.string.search_best_result)) }
                        item { SearchResultItem(onClick = onItemClick) }

                        // Content groups
                        item { ResultHeader(stringResource(R.string.search_section_albums)) }
                        items(3) { SearchResultItem(onClick = onItemClick) }

                        item { ResultHeader(stringResource(R.string.search_section_artists)) }
                        items(3) { SearchResultItem(isArtist = true, onClick = onItemClick) }

                        item { ResultHeader(stringResource(R.string.search_section_playlists)) }
                        items(4) { SearchResultItem(onClick = onItemClick) }
                    }
                    SearchCategory.ARTISTS -> {
                        items(12) { SearchResultItem(isArtist = true, onClick = onItemClick) }
                    }
                    else -> {
                        // Songs and Videos
                        items(15) { SearchResultItem(onClick = onItemClick) }
                    }
                }
            }
        }
    }
}

// Result section header
@Composable
private fun ResultHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}
