package com.lonewolf.wavvy.ui.search.results

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project components
import com.lonewolf.wavvy.ui.search.results.components.SearchResultList
import com.lonewolf.wavvy.ui.theme.Poppins

// Screen to display search results
@Composable
fun SearchResultScreen(
    query: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Temp debug text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Resultados para:",
                fontFamily = Poppins,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Text(
                text = query,
                fontFamily = Poppins,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results list container
        SearchResultList(
            query = query,
            onItemClick = { /* Handle song play */ }
        )
    }
}
