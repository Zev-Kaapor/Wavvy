package com.wavvy.app.features.home.ui.components

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
import com.wavvy.app.core.designsystem.theme.Poppins

// User greeting section
@Composable
fun GreetingSection(
    userName: String?,
    greetingTemplate: String,
    question: String,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Main greeting animation
        AnimatedContent(
            targetState = userName,
            transitionSpec = {
                fadeIn(animationSpec = tween(550)) togetherWith
                        fadeOut(animationSpec = tween(350))
            },
            label = "greeting_text_transition"
        ) { targetUserName ->
            // Styled greeting logic
            val renderingState = remember(targetUserName, highlightColor, greetingTemplate) {
                buildAnnotatedString {
                    val hasUser = !targetUserName.isNullOrBlank()

                    val cleanTemplate = if (hasUser) {
                        greetingTemplate
                    } else {
                        greetingTemplate
                            .replace(", {user}", "")
                            .replace(" {user}", "")
                            .replace("{user}", "")
                            .trim()
                    }

                    if (hasUser && cleanTemplate.contains("{user}")) {
                        val parts = cleanTemplate.split("{user}")
                        append(parts[0])
                        withStyle(
                            style = SpanStyle(
                                color = highlightColor,
                                fontWeight = FontWeight.Black
                            )
                        ) {
                            append(targetUserName)
                        }
                        if (parts.size > 1) {
                            append(parts[1])
                        }
                    } else {
                        append(cleanTemplate)
                    }
                }
            }

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

        // Subtitle text
        Text(
            text = question.trim(),
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
