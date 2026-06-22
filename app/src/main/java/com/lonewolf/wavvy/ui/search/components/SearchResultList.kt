package com.lonewolf.wavvy.ui.search.components

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// UI utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.SearchCategory
import com.lonewolf.wavvy.ui.theme.Poppins

// Data model for generic search result items
data class SearchResultData(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val isArtist: Boolean = false
)

// Coordinator for search result lists
@Composable
fun SearchResultList(
    category: SearchCategory,
    results: List<SearchResultData>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (results.isEmpty()) {
            // Clean and subtle empty state for results
            SearchEmptyState()
        } else {
            if (category == SearchCategory.ALBUMS) {
                // Grid layout for albums
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(items = results, key = { it.id }) { album ->
                        AlbumGridItem(onClick = { onItemClick(album.id) })
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
                            results.firstOrNull()?.let { best ->
                                item { SearchResultItem(isArtist = best.isArtist, onClick = { onItemClick(best.id) }) }
                            }

                            // Albums Group
                            val albums = results.filter { !it.isArtist } // Exemplo de lógica de filtro
                            if (albums.isNotEmpty()) {
                                item { ResultHeader(stringResource(R.string.search_section_albums)) }
                                items(items = albums.take(3), key = { "all_album_${it.id}" }) { album ->
                                    SearchResultItem(onClick = { onItemClick(album.id) })
                                }
                            }

                            // Artists Group
                            val artists = results.filter { it.isArtist }
                            if (artists.isNotEmpty()) {
                                item { ResultHeader(stringResource(R.string.search_section_artists)) }
                                items(items = artists.take(3), key = { "all_artist_${it.id}" }) { artist ->
                                    SearchResultItem(isArtist = true, onClick = { onItemClick(artist.id) })
                                }
                            }
                        }
                        SearchCategory.ARTISTS -> {
                            items(items = results, key = { it.id }) { item ->
                                SearchResultItem(isArtist = true, onClick = { onItemClick(item.id) })
                            }
                        }
                        else -> {
                            // Songs and Videos
                            items(items = results, key = { it.id }) { item ->
                                SearchResultItem(isArtist = item.isArtist, onClick = { onItemClick(item.id) })
                            }
                        }
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

// Minimalist clean empty state for search
@Composable
fun SearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.search_empty_state),
            fontFamily = Poppins,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
