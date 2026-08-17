package com.example.myapplication.ui.screens

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme 
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    MyApplicationTheme {
        SplashScreen(onNavigateToHome = {})
    }
}

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = LinearOutSlowInEasing)
            )
        }
        delay(3000)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo Section
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .size(140.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                EditorLogo()
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Title Section
            Text(
                text = "Modern\nText Editor",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Incremental Version Control",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Tagline Section
            Text(
                text = "Code. Write. Version. Recover.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Progress Indicator
            ModernProgressIndicator(modifier = Modifier.alpha(alpha.value))
        }

        // Footer Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Initializing Editor...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun EditorLogo() {
    val accentColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = Modifier.size(70.dp)) {
        val strokeWidth = 6.dp.toPx()
        
        // <
        drawLine(
            color = accentColor,
            start = Offset(25.dp.toPx(), 25.dp.toPx()),
            end = Offset(10.dp.toPx(), 35.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor,
            start = Offset(10.dp.toPx(), 35.dp.toPx()),
            end = Offset(25.dp.toPx(), 45.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // /
        drawLine(
            color = accentColor,
            start = Offset(45.dp.toPx(), 15.dp.toPx()),
            end = Offset(25.dp.toPx(), 55.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // >
        drawLine(
            color = accentColor,
            start = Offset(45.dp.toPx(), 25.dp.toPx()),
            end = Offset(60.dp.toPx(), 35.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor,
            start = Offset(60.dp.toPx(), 35.dp.toPx()),
            end = Offset(45.dp.toPx(), 45.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ModernProgressIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val activeIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "index"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) { index ->
            val isActive = index == activeIndex % 4
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
