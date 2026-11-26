package com.parsaapps.nichmarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parsaapps.nichmarket.util.toPersian

/** Consistent illustrations for category identities throughout the app. */
fun categoryIcon(categoryId: String): ImageVector = when (categoryId) {
    "amoozesh" -> Icons.Rounded.School
    "bank" -> Icons.Rounded.AccountBalance
    "dadgostari" -> Icons.Rounded.Gavel
    else -> Icons.Rounded.AutoStories
}

/** Branded header shared by the secondary destinations. */
@Composable
fun GradientHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()
        .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF102B2C), Color(0xFF175F54))))) {
        Box(Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp).size(110.dp)
            .border(1.dp, Color.White.copy(alpha = .12f), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).padding(end = 44.dp, top = 40.dp).size(46.dp)
            .background(Color(0xFFD9B977).copy(alpha = .16f), CircleShape))
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("نیچ مارکت  /  پارسا اپس", style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFEFD09A), fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color.White)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0EDE5))
            }
        }
    }
}

/** کارت آمار کوچک (درست / غلط / بی‌جواب) */
@Composable
fun StatCard(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.toPersian(),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** سطر اطلاعات متنی (کلید/مقدار) */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
