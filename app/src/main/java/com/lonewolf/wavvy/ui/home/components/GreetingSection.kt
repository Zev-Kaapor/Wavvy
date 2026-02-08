package com.lonewolf.wavvy.ui.home.components

import java.util.Calendar
// Compose layouts and foundations
import androidx.compose.foundation.layout.*
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// State and composition utilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// User greeting section with dynamic time-based message
@Composable
fun GreetingSection(
    userName: String?,
    modifier: Modifier = Modifier
) {
    val greeting = rememberGreeting()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Annotated string for styled user name
    val annotatedGreeting = remember(greeting, userName, tertiaryColor) {
        buildAnnotatedString {
            append(greeting)
            if (!userName.isNullOrBlank()) {
                append(", ")
                withStyle(
                    style = SpanStyle(
                        color = tertiaryColor,
                        fontWeight = FontWeight.Black
                    )
                ) {
                    append(userName)
                }
            }
            append("!")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Main greeting
        Text(
            text = annotatedGreeting,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        // Subtitle question
        Text(
            text = stringResource(R.string.greeting_question),
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// Logic to determine greeting based on system time
@Composable
private fun rememberGreeting(): String {
    val morning = stringResource(R.string.greeting_morning)
    val afternoon = stringResource(R.string.greeting_afternoon)
    val evening = stringResource(R.string.greeting_evening)

    return remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> morning
            in 12..17 -> afternoon
            else -> evening
        }
    }
}
