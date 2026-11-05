/*
 * CS Launcher Lite
 *
 * Derived from Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.cs.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.legal.AvailableAgreement
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.cs.components.CsPrimaryButton
import com.movtery.zalithlauncher.ui.cs.theme.CsTheme
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.movtery.zalithlauncher.ui.screens.splash.AgreementDocumentDialog
import com.movtery.zalithlauncher.ui.screens.splash.SplashOperation
import com.movtery.zalithlauncher.viewmodel.SplashBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 首次启动欢迎屏幕（双栏布局）：
 * 左侧祖母绿欢迎面板，右侧应用信息、用户协议与开始按钮。
 *
 * 协议逻辑完全复用启动器已有的 [AvailableAgreement] 数据流，
 * 只有阅读并勾选协议（若存在协议）后才允许继续。
 */
@Composable
fun CsWelcomeScreen(
    onFinished: () -> Unit,
    availableAgreement: StateFlow<AvailableAgreement?>,
    screenViewModel: SplashBackStackViewModel,
    modifier: Modifier = Modifier
) {
    CsTheme {
    BaseScreen(
        screenKey = SplashOperation.Welcome,
        currentKey = screenViewModel.splashScreen.currentKey
    ) { isVisible ->
        val agreement by availableAgreement.collectAsStateWithLifecycle()
        var started by remember { mutableStateOf(false) }
        var agreementRequired by remember(agreement) { mutableStateOf(agreement != null) }
        var agreementChecked by remember(agreement) { mutableStateOf(false) }

        val onStart: () -> Unit = {
            if (!started && (!agreementRequired || agreementChecked)) {
                started = true
                onFinished()
            }
        }

        val appear by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            label = "csWelcomeAppear"
        )

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(CsColors.Background)
                .padding(16.dp)
        ) {
            val isCompact = maxWidth < 720.dp
            val gap = if (isCompact) 12.dp else 18.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(appear),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                // 左侧：欢迎面板
                WelcomeBrandPanel(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(if (isCompact) 220.dp else (maxWidth * 0.38f).coerceIn(280.dp, 440.dp))
                )

                // 右侧：信息 + 协议 + 开始按钮
                WelcomeInfoPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    availableAgreement = availableAgreement,
                    agreementChecked = agreementChecked,
                    onAgreementCheckedChange = { agreementChecked = it },
                    onAgreementRequiredChange = { agreementRequired = it },
                    started = started,
                    canStart = !agreementRequired || agreementChecked,
                    onStart = onStart
                )
            }
        }
    }
    }
}

/**
 * 左侧祖母绿面板
 */
@Composable
private fun WelcomeBrandPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CsColors.Accent,
                        CsColors.AccentDark,
                        Color(0xFF0A3F23)
                    )
                )
            )
    ) {
        // 装饰图形
        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(x = (-70).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(Color.Black.copy(alpha = 0.14f))
        )
        Box(
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 34.dp)
                .rotate(18f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.07f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B2A17))
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.cs_welcome_made_with),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    ),
                    color = Color(0xFF0B2A17).copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.cs_welcome_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = Color(0xFF04170C),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                modifier = Modifier.widthIn(max = 300.dp),
                text = stringResource(R.string.cs_welcome_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF06230F).copy(alpha = 0.85f),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 右侧信息面板
 */
@Composable
private fun WelcomeInfoPanel(
    availableAgreement: StateFlow<AvailableAgreement?>,
    agreementChecked: Boolean,
    onAgreementCheckedChange: (Boolean) -> Unit,
    onAgreementRequiredChange: (Boolean) -> Unit,
    started: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val agreement by availableAgreement.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var documents by remember(agreement) {
        mutableStateOf<List<AvailableAgreement.DocumentFile.Data>>(emptyList())
    }
    var selectedDocument by remember(agreement) {
        mutableStateOf<AvailableAgreement.DocumentFile.Data?>(null)
    }

    LaunchedEffect(agreement) {
        val region = if (Locale.getDefault().country.equals("CN", ignoreCase = true)) "zh" else "global"
        val selectedAgreement = agreement?.files?.firstOrNull {
            it.effectiveRegion.equals(region, ignoreCase = true)
        } ?: agreement?.files?.firstOrNull {
            it.effectiveRegion.equals("global", ignoreCase = true)
        }

        val availableDocuments = withContext(Dispatchers.IO) {
            selectedAgreement?.dataList.orEmpty().filter {
                it.title.isNotBlank() && it.path.isNotBlank() && runCatching {
                    context.assets.open(it.path).close()
                }.isSuccess
            }
        }
        documents = availableDocuments
        onAgreementRequiredChange(availableDocuments.isNotEmpty())
        if (availableDocuments.isEmpty()) onAgreementCheckedChange(false)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CsColors.BackgroundSecondary)
            .border(1.dp, CsColors.Border, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 应用图标 + 名称
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(CsColors.AccentSoft)
                        .border(1.dp, CsColors.AccentDark, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        painter = painterResource(R.drawable.ic_cs_logo),
                        contentDescription = null,
                        tint = CsColors.Accent
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = BuildKeys.LAUNCHER_NAME,
                        style = MaterialTheme.typography.headlineSmall,
                        color = CsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.cs_discover_source_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.welcome_screen_intro_primary, BuildKeys.LAUNCHER_NAME),
                style = MaterialTheme.typography.bodyLarge,
                color = CsColors.TextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cs_welcome_short_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = CsColors.TextTertiary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (documents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.welcome_screen_agreement_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    documents.forEach { document ->
                        InfoLayoutTextItem(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedDocument = document },
                            title = document.title
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = agreementChecked,
                        onCheckedChange = onAgreementCheckedChange
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.welcome_screen_agreement_confirmation),
                        style = MaterialTheme.typography.bodySmall,
                        color = CsColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 开始按钮位于内容流末尾，永远不会覆盖协议卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                text = stringResource(R.string.cs_welcome_get_started),
                style = MaterialTheme.typography.labelSmall,
                color = CsColors.TextTertiary,
                maxLines = 1,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )
            CsPrimaryButton(
                text = if (started) {
                    stringResource(R.string.cs_welcome_starting)
                } else {
                    stringResource(R.string.welcome_screen_start)
                },
                onClick = onStart,
                enabled = canStart && !started,
                iconRes = R.drawable.ic_cs_chevron_right
            )
        }
    }

    selectedDocument?.let { document ->
        AgreementDocumentDialog(
            document = document,
            onDismissRequest = { selectedDocument = null }
        )
    }
}
