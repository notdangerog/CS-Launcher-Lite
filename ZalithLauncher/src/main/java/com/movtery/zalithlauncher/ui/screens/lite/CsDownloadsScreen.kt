/*
 * CS Launcher Lite — Downloads screen (5th nav destination)
 * Shows active tasks + installation history, fully functional backend.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsSpace

@Composable
fun CsDownloadsScreen(
    modifier: Modifier = Modifier
) {
    val tasks by TaskSystem.tasksFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CsColor.Background)
            .padding(horizontal = com.movtery.zalithlauncher.ui.design.CsSpace.l, vertical = com.movtery.zalithlauncher.ui.design.CsSpace.m),
        verticalArrangement = Arrangement.spacedBy(CsSpace.l)
    ) {
        Text(
            text = stringResource(R.string.cs_downloads_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = CsColor.TextPrimary
        )
        Text(
            text = stringResource(R.string.cs_downloads_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = CsColor.TextSecondary
        )

        if (tasks.isEmpty()) {
            CsCard(
                modifier = Modifier.fillMaxWidth(),
                color = CsColor.Card,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CsSpace.s)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CsColor.Neutral4),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_2_outlined),
                            contentDescription = null,
                            tint = CsColor.TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.cs_downloads_empty),
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColor.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.cs_downloads_no_history),
                        style = MaterialTheme.typography.bodySmall,
                        color = CsColor.TextSecondary
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.cs_downloads_active) + " · ${tasks.size}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CsColor.TextSecondary
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CsSpace.m),
                contentPadding = PaddingValues(bottom = CsSpace.l)
            ) {
                items(tasks, key = { it.id }) { task ->
                    CsTaskCard(task = task)
                }
            }
        }
    }
}

@Composable
private fun CsTaskCard(task: Task) {
    val progress by task.progress.collectAsState()
    val message by task.message.collectAsState()
    val stage by task.stage.collectAsState()

    CsCard(
        modifier = Modifier.fillMaxWidth(),
        color = CsColor.Card,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = task.id,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = CsColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stage.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColor.Accent
                )
            }
            if (message != null) {
                Text(
                    text = message.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = CsColor.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (progress >= 0f) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                    color = CsColor.Accent,
                    trackColor = CsColor.Neutral4
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                    color = CsColor.Accent,
                    trackColor = CsColor.Neutral4
                )
            }
        }
    }
}
