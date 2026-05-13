package com.example.meetwise_ai_scheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import com.example.meetwise_ai_scheduler.ui.navigation.NavGraph
import com.example.meetwise_ai_scheduler.ui.theme.MeetWiseAISchedulerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember {
                getSharedPreferences("meetwise_preferences", MODE_PRIVATE)
            }
            var darkTheme by remember {
                mutableStateOf(preferences.getBoolean("dark_theme", false))
            }
            var largeText by remember {
                mutableStateOf(preferences.getBoolean("large_text", false))
            }
            var reduceMotion by remember {
                mutableStateOf(preferences.getBoolean("reduce_motion", false))
            }
            val density = LocalDensity.current
            MeetWiseAISchedulerTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = if (largeText) 1.15f else 1f
                    )
                ) {
                    NavGraph(
                        isDarkTheme = darkTheme,
                        largeText = largeText,
                        reduceMotion = reduceMotion,
                        onToggleTheme = {
                            darkTheme = !darkTheme
                            preferences.edit().putBoolean("dark_theme", darkTheme).apply()
                        },
                        onToggleLargeText = {
                            largeText = !largeText
                            preferences.edit().putBoolean("large_text", largeText).apply()
                        },
                        onToggleReduceMotion = {
                            reduceMotion = !reduceMotion
                            preferences.edit().putBoolean("reduce_motion", reduceMotion).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MeetWiseAISchedulerTheme {
        Greeting("Android")
    }
}
