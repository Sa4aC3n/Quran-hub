package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerState
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.components.IslamicGeometricBackground
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    playerState: PlayerState,
    getPlaylistItems: (String) -> Flow<List<PlaylistItem>>,
    onCreatePlaylist: (name: String, description: String, colorHex: String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayPlaylist: (playlist: Playlist, startIndex: Int, shuffle: Boolean) -> Unit,
    onRemovePlaylistItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IslamicGeometricBackground(modifier = Modifier.fillMaxSize())

        if (selectedPlaylist != null) {
        val currentPl = selectedPlaylist!!
        val itemsFlow = remember(currentPl.id) { getPlaylistItems(currentPl.id) }
        val items by itemsFlow.collectAsState(initial = emptyList())

        PlaylistDetailView(
            playlist = currentPl,
            items = items,
            playerState = playerState,
            onBack = { selectedPlaylist = null },
            onPlayAll = { shuffle -> onPlayPlaylist(currentPl, 0, shuffle) },
            onPlayItem = { index -> onPlayPlaylist(currentPl, index, false) },
            onDeleteItem = onRemovePlaylistItem,
            onDeletePlaylist = {
                onDeletePlaylist(currentPl.id)
                selectedPlaylist = null
            }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_playlist")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إنشاء قائمة تشغيل جديدة")
                }
            },
            containerColor = Color.Transparent,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "قوائم التشغيل المخصصة 🗂️",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "أنشئ قوائم تلاوة خاصة بك (مثل: أذكار الصباح، ورد الحفظ، قيام الليل)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لا توجد قوائم تشغيل مخصصة بعد",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "اضغط على زر (+) في الأسفل لإنشاء قائمة تشغيل وإضافة سور بأصوات قرائك المفضلين إليها",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنشاء قائمة جديدة")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                isCurrentlyPlaying = playerState.activePlaylistId == playlist.id,
                                onClick = { selectedPlaylist = playlist },
                                onPlay = { onPlayPlaylist(playlist, 0, false) },
                                onDelete = { onDeletePlaylist(playlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Sheet
    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }
        var colorHex by remember { mutableStateOf("#10B981") }

        ModalBottomSheet(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "إنشاء قائمة تشغيل جديدة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("اسم القائمة (مثلاً: تلاوات خاشعة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = playlistDesc,
                    onValueChange = { playlistDesc = it },
                    label = { Text("وصف القائمة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "اختر لون القائمة المميز:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))

                val colors = listOf("#10B981", "#3B82F6", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899", "#14B8A6")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { hex ->
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .clickable { colorHex = hex }
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName, playlistDesc, colorHex)
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = playlistName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("إنشاء القائمة")
                }
            }
        }
    }
}
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isCurrentlyPlaying) androidx.compose.foundation.BorderStroke(1.5.dp, Gold500) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(parseHexColor(playlist.colorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = parseHexColor(playlist.colorHex),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (playlist.description.isNotBlank()) "${playlist.description} • ${playlist.itemsCount} سور"
                               else "${playlist.itemsCount} سور مسجلة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playlist.itemsCount > 0) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تشغيل القائمة",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    items: List<PlaylistItem>,
    playerState: PlayerState,
    onBack: () -> Unit,
    onPlayAll: (shuffle: Boolean) -> Unit,
    onPlayItem: (Int) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeletePlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${items.size} سور في هذه القائمة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDeletePlaylist) {
                Icon(Icons.Default.Delete, contentDescription = "حذف القائمة", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Play and Shuffle buttons
        if (items.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onPlayAll(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تشغيل الكل")
                }
                OutlinedButton(
                    onClick = { onPlayAll(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تشغيل عشوائي")
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Items List
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "القائمة فارغة حالياً 📖",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك إضافة السور إلى هذه القائمة مباشرة من المشغل الصوتي عبر زر (+) 'إضافة لقائمة تشغيل'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val isPlayingThis = playerState.activePlaylistId == playlist.id &&
                            playerState.currentItem?.surahNumber == item.surahNumber &&
                            playerState.currentItem?.reciterId == item.reciterId

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isPlayingThis) androidx.compose.foundation.BorderStroke(1.dp, Gold500) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayItem(index) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "سورة ${item.surahName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.reciterName} • ${item.riwayah}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteItem(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف من القائمة",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF0B1F3A)
    }
}
