package com.newspulse.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.Typography

@Composable
fun MetricCard(
    title: String,
    value: String,
    valueColor: Color = OnSurfacePrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineDivider, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = Typography.labelSmall,
            color = OnSurfaceSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = Typography.headlineSmall,
            color = valueColor
        )
    }
}
