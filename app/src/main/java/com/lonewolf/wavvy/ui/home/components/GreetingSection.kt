package com.lonewolf.wavvy.ui.home.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.*
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// State and composition utilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins

// User greeting section with dynamic time-based message
@Composable
fun GreetingSection(
    userName: String?,
    greeting: String,
    question: String,
    modifier: Modifier = Modifier
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Annotated string for styled username
    val annotatedGreeting = remember(greeting, userName, tertiaryColor) {
        buildAnnotatedString {
            val lastChar = greeting.lastOrNull()
            val isQuestion = lastChar == '?'
            val isExclamation = lastChar == '!'

            // Clean the greeting from ending punctuation for middle-sentence flow
            val cleanGreeting = if (isQuestion || isExclamation) {
                greeting.dropLast(1)
            } else {
                greeting
            }

            append(cleanGreeting)

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

            // Move the original or default punctuation to the very end
            when {
                isQuestion -> append("?")
                isExclamation -> append("!")
                else -> append("!")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Line 1: Casual Greeting + Name
        Text(
            text = annotatedGreeting,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        // Line 2: Contextual Question
        Text(
            text = question,
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
