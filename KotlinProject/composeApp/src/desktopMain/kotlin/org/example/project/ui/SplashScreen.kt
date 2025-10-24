package org.example.project.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

// Custom shape for left-to-right reveal
class LeftToRightRevealShape(private val revealFraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            Rect(
                left = 0.5f,
                top = 0.5f,
                right = size.width * revealFraction,
                bottom = size.height
            )
        )
    }
}

@Composable
fun SplashScreenDesktop(onFinished: () -> Unit) {
    // Main content animations
    val mainScale = remember { Animatable(0.9f) }
    val mainAlpha = remember { Animatable(0f) }

    // Tagline animation - reveal fraction from left to right
    val taglineReveal = remember { Animatable(0f) }

    // Original golden colors
    val goldTop = Color(0xFFC9AD6E)
    val goldBottom = Color(0xFFB8973D)

    // Animation sequence
    LaunchedEffect(Unit) {
        coroutineScope {
            // First phase: main content fade in and slow zoom
            launch {
                // Gentle fade in
                mainAlpha.animateTo(1f, animationSpec = tween(1000, easing = EaseOutQuad))
            }

            launch {
                // Concurrent slow zoom from 90% to 100%
                mainScale.animateTo(1f, animationSpec = tween(1000, easing = EaseOutQuad))
            }

            // Second phase: tagline reveal from left to right
            launch {
                delay(700) // Start after main content begins to appear
                taglineReveal.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
            }
        }
    }

    // Splash screen timing - 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    // Main container with original gold background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        goldTop,
                        goldBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Fixed size content container to prevent layout shifts
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Content column - fixed height to prevent movement
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .scale(mainScale.value)
                    .alpha(mainAlpha.value)
            ) {
                // Diamond icon
                Icon(
                    Icons.Default.Diamond,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )

                // Company name and type
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 32.dp)
                ) {
                    // Company name
                    Text(
                        text = "Vishal Gems & Jewels",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    // Company type
                    Text(
                        text = "PVT LTD",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Space between main content and tagline - fixed height
                Spacer(modifier = Modifier.height(24.dp))

                // Tagline container with fixed dimensions
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(20.dp)
                        .width(260.dp) // Fixed width to ensure stability
                ) {
                    // First, we place the full tagline (invisible)
                    Text(
                        text = "ELEVATE YOUR STYLE",
                        color = Color.White.copy(alpha = 0.0f), // Invisible base layer for sizing
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )

                    // Then, we place the revealed tagline over it
                    Text(
                        text = "ELEVATE YOUR STYLE",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(LeftToRightRevealShape(taglineReveal.value))
                    )
                }
            }
        }
    }
}