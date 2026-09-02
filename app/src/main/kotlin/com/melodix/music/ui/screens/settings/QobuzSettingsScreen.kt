/**
 * Melodix (C) 2026 — "Feel Every Note"
 * Licensed under GPL-3.0. Qobuz lossless settings.
 */

package com.melodix.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.melodix.music.R
import com.melodix.music.constants.EnableQobuzKey
import com.melodix.music.constants.QobuzAudioQuality
import com.melodix.music.constants.QobuzAudioQualityKey
import com.melodix.music.constants.QobuzBackendKey
import com.melodix.music.constants.QobuzCountryKey
import com.melodix.music.qobuz.QobuzAudioProvider
import com.melodix.music.ui.component.PreferenceEntry
import com.melodix.music.ui.utils.backToMain
import com.melodix.music.utils.rememberEnumPreference
import com.melodix.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QobuzSettingsScreen(navController: NavController) {
    val (enabled, onEnabledChange) = rememberPreference(EnableQobuzKey, false)
    val (quality, onQualityChange) =
        rememberEnumPreference(QobuzAudioQualityKey, QobuzAudioQuality.CD_QUALITY)
    val (backend, onBackendChange) = rememberPreference(QobuzBackendKey, "MONOKENNY")
    val (country, onCountryChange) = rememberPreference(QobuzCountryKey, "US")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qobuz_enable)) },
                navigationIcon = {
                    M3IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.qobuz_enable_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PreferenceEntry(
                title = { Text(stringResource(R.string.qobuz_enable)) },
                description = stringResource(R.string.qobuz_enable_description),
                icon = { Icon(painterResource(R.drawable.settings), null) },
                trailingContent = { Switch(checked = enabled, onCheckedChange = onEnabledChange) },
                onClick = { onEnabledChange(!enabled) },
            )

            Text(
                text = stringResource(R.string.qobuz_quality),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            QobuzAudioQuality.entries.forEach { q ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RadioButton(
                        selected = quality == q,
                        onClick = { onQualityChange(q) },
                    )
                    Text(
                        text =
                            when (q) {
                                QobuzAudioQuality.AAC_320 -> stringResource(R.string.qobuz_quality_aac_320)
                                QobuzAudioQuality.CD_QUALITY -> stringResource(R.string.qobuz_quality_cd)
                                QobuzAudioQuality.HI_RES_LOSSLESS -> stringResource(R.string.qobuz_quality_hires)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Text(
                text = stringResource(R.string.qobuz_backend),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            QobuzAudioProvider.ResolverBackend.entries.forEach { b ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RadioButton(
                        selected = backend == b.name,
                        onClick = { onBackendChange(b.name) },
                    )
                    Text(b.name, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = country,
                onValueChange = { if (it.length <= 2) onCountryChange(it.uppercase()) },
                label = { Text(stringResource(R.string.qobuz_country)) },
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.qobuz_country_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
