package com.newspulse.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newspulse.ai.ui.MainViewModel
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OnSurfaceSecondary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.SurfaceContainerLow
import com.newspulse.ai.ui.theme.Typography
import com.newspulse.ai.ui.theme.WarningContainer
import com.newspulse.ai.ui.theme.WarningContent
import com.newspulse.ai.ui.theme.WatchContainer
import com.newspulse.ai.ui.theme.WatchContent

@Composable
fun FilingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val filings by viewModel.filings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBase)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exchange Disclosures",
                style = Typography.headlineSmall,
                color = OnSurfacePrimary
            )
        }

        Divider(color = OutlineDivider, thickness = 1.dp)

        if (filings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exchange filings cached.\nFilings auto-populate during active market surveillance.",
                    style = Typography.bodyMedium,
                    color = OnSurfaceSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filings, key = { it.hash }) { filing ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceBase)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = filing.symbol,
                                        style = Typography.titleSmall,
                                        color = OnSurfacePrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "•",
                                        color = OnSurfaceDisabled,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = filing.source.uppercase(),
                                        style = Typography.bodySmall,
                                        color = OnSurfaceSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(WarningContainer, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = filing.filingType,
                                        style = Typography.labelSmall,
                                        color = WarningContent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = filing.title,
                                style = Typography.titleMedium,
                                color = OnSurfacePrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = filing.publishedAt,
                                style = Typography.bodySmall,
                                color = OnSurfaceDisabled
                            )
                        }

                        Divider(color = OutlineDivider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}
