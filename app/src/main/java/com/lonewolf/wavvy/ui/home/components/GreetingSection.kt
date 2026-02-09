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
import androidx.compose.ui.platform.LocalContext
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
    val (greeting, question) = rememberGreetingAndQuestion()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Annotated string for styled user name
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

@Composable
private fun rememberGreetingAndQuestion(): Pair<String, String> {
    val context = LocalContext.current

    return remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Time slots based on the provided guide
        val (resGreetings, resQuestions) = when (hour) {
            in 0..5 -> R.array.dawn_greetings to R.array.dawn_questions
            in 6..11 -> R.array.morning_greetings to R.array.morning_questions
            in 12..17 -> R.array.afternoon_greetings to R.array.afternoon_questions
            else -> R.array.evening_greetings to R.array.evening_questions
        }

        val greetings = context.resources.getStringArray(resGreetings)
        val questions = context.resources.getStringArray(resQuestions)

        // Random pick for variety
        greetings.random() to questions.random()
    }
}
