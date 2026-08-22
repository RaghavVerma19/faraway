package com.newspulse.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.data.model.SeverityTier
import com.newspulse.ai.ui.theme.CrashContent
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.TwitterBlue
import com.newspulse.ai.ui.theme.WarningContent

@Composable
fun TierBadge(
    tier: SeverityTier,
    modifier: Modifier = Modifier
) {
    val dotColor = when (tier) {
        SeverityTier.CRITICAL -> CrashContent
        SeverityTier.HIGH -> WarningContent
        SeverityTier.WATCH -> TwitterBlue
        SeverityTier.IGNORE -> OnSurfaceSecondary
    }

    Box(
        modifier = modifier
            .background(SurfaceContainerLow, RoundedCornerShape(999.dp))
            .border(1.dp, OutlineDivider, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = tier.name,
                color = OnSurfaceSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}
