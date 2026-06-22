package com.lonewolf.wavvy.ui.home.components

// Compose layouts and foundations
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

    // Check intent before cleaning to establish the flow
    val isGreetingQuestion = greetingTemplate.contains("?")

    // Styled greeting logic
    val annotatedGreeting = remember(greetingTemplate, userName, highlightColor) {
        buildAnnotatedString {
            val finalMark = if (isGreetingQuestion) "?" else "!"

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

    // Dynamic punctuation rule for the subtext based on the primary line
    val filteredQuestion = remember(isGreetingQuestion, question) {
        val targetMark = if (isGreetingQuestion) "." else "?"

        val cleanQuestion = question
            .trim()
            .replace(Regex("[?.!]$"), "") // Strip existing trailing punctuation safely
            .trim()

        "$cleanQuestion$targetMark"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Line 1: Main greeting with crossfade transition to prevent layout snaps - fine-tuned duration
        AnimatedContent(
            targetState = userName,
            transitionSpec = {
                fadeIn(animationSpec = tween(550)) togetherWith
                        fadeOut(animationSpec = tween(350))
            },
            label = "greeting_text_transition"
        ) { targetUserName ->
            // Binding targetUserName directly to verify composition state tracking
            val renderingState = remember(targetUserName) { annotatedGreeting }

            Text(
                text = renderingState,
                style = TextStyle(
                    fontFamily = Poppins,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

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
