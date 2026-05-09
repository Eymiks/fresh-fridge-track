package com.freshtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freshtrack.ui.theme.FreshTextStyles

@Composable
fun ProductSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean? = null,
    onToggle: (() -> Unit)? = null
) {
    val clickableModifier = if (onToggle != null) {
        Modifier.clickable(onClick = onToggle)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .then(clickableModifier)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            title.uppercase(),
            style = FreshTextStyles.SectionHeader,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            "($count)",
            style = FreshTextStyles.SectionCount,
            color = color.copy(alpha = 0.56f)
        )
        Spacer(Modifier.weight(1f))
        if (isCollapsed != null && onToggle != null) {
            Icon(
                if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "Déplier" else "Replier",
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
