package com.e9ab98.kmprecording.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.e9ab98.kmprecording.domain.L10n
import com.e9ab98.kmprecording.domain.RecordingLifecycle
import com.e9ab98.kmprecording.domain.RecordingMode
import com.e9ab98.kmprecording.presentation.RecordingUiState
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun RecordingOverlay(
    state: RecordingUiState,
    modifier: Modifier = Modifier
) {
    val l10n = L10n.get(state.appLanguage)

    Box(modifier = modifier) {
        // Top-Left: Recording status & Duration & Timestamp
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.lifecycle is RecordingLifecycle.Recording) {
                    BlinkingRecBadge()
                }
                BadgeText(formatDuration(state.elapsedSeconds))
            }
            if (state.timestampText.isNotEmpty()) {
                BadgeText(state.timestampText)
            }
        }

        // Top-End (Wait, RightControlDeck is on the right, so we shouldn't overlap. We can put this at Top-Center or Top-Start below others)
        // Let's put resolution and mode in a row at Top-Center to avoid clashing with the RightControlDeck
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BadgeText("${state.resolution.height}P")
            BadgeText(if (state.cameraType.name == "BACK") l10n.cameraRear else l10n.cameraFront)
        }

        // Bottom-Start: HUD Overlay (Speed & GPS Dashboard)
        if (state.showHud) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top row of the dashboard: Speed and Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Speed display
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = state.speedKmh.formatDecimal(0),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 36.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Mode & GPS Status tags
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Loop Mode tag
                        if (state.mode == RecordingMode.LOOP) {
                            val loopText = state.activeSegmentIndex?.let { "${l10n.modeLoop} #${it + 1}" } ?: l10n.modeLoop
                            Text(
                                text = loopText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64B5F6),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // GPS Active tag
                        val gpsColor = if (state.latitude != null) Color(0xFF81C784) else Color(0xFFE57373)
                        val gpsText = if (state.latitude != null) "GPS ACTIVE" else "GPS SEARCHING"
                        Text(
                            text = gpsText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = gpsColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Coordinates block
                Column {
                    val latStr = state.latitude?.let { lat ->
                        val dir = if (lat >= 0) "N" else "S"
                        "LAT: ${abs(lat).formatDecimal(5)}° $dir"
                    } ?: "LAT: --° N"
                    
                    val lonStr = state.longitude?.let { lon ->
                        val dir = if (lon >= 0) "E" else "W"
                        "LON: ${abs(lon).formatDecimal(5)}° $dir"
                    } ?: "LON: --° E"

                    Text(
                        text = latStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = lonStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            if (state.mode == RecordingMode.LOOP) {
                BadgeText(
                    text = state.activeSegmentIndex?.let { "${l10n.modeLoop} #${it + 1}" } ?: l10n.modeLoop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BlinkingRecBadge() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Red)
                .alpha(alpha)
        )
        Text(
            text = "REC",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
private fun BadgeText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }
}

private fun Double.formatDecimal(decimals: Int): String {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    val rounded = (this * multiplier).roundToInt() / multiplier
    val str = rounded.toString()
    val parts = str.split(".")
    if (parts.size == 2) {
        val frac = parts[1]
        if (frac.length < decimals) {
            return "${parts[0]}.${frac.padEnd(decimals, '0')}"
        }
    } else if (parts.size == 1 && decimals > 0) {
        return "$str.${"0".repeat(decimals)}"
    }
    return str
}
