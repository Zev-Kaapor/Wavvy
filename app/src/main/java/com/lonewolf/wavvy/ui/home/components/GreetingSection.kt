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

// User greeting section with dynamic template-based message
@Composable
fun GreetingSection(
    userName: String?,
    greetingTemplate: String,
    question: String,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val hasName = !userName.isNullOrBlank()

    // Simplified styled greeting
    val annotatedGreeting = remember(greetingTemplate, userName, highlightColor) {
        buildAnnotatedString {
            // Check intent before cleaning
            val isQuestion = greetingTemplate.contains("?")
            val finalMark = if (isQuestion) "?" else "!"

            // Strip current punctuation and placeholder for a clean base
            val baseText = greetingTemplate
                .replace("?", "")
                .replace("!", "")
                .replace(", {user}", "")
                .replace("{user}", "")
                .trim()

            append(baseText)

            if (hasName) {
                append(", ")
                withStyle(
                    style = SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Black
                    )
                ) {
                    append(userName!!)
                }
            }
            append(finalMark)
        }
    }

    // Punctuation logic to avoid double marks in sequence
    val filteredQuestion = remember(annotatedGreeting, question) {
        val lastCharGreeting = annotatedGreeting.text.lastOrNull()
        val firstCharQuestion = question.trim().firstOrNull()

        if (lastCharGreeting == firstCharQuestion && (lastCharGreeting == '?' || lastCharGreeting == '!')) {
            question.trim().drop(1).toString()
        } else {
            question
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Line 1: Main greeting
        Text(
            text = annotatedGreeting,
            style = TextStyle(
                fontFamily = Poppins,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        // Line 2: Subtitle question
        Text(
            text = filteredQuestion,
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
