package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PlayerState
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import com.example.ui.components.IslamicGeometricBackground
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.ui.theme.Gold500
import kotlinx.coroutines.flow.Flow

@Composable
fun FavoritesScreen(
    favoriteReciters: List<Reciter>,
    favoriteSurahs: List<Surah>,
    favoriteReciterIds: Set<String>,
    favoriteSurahNumbers: Set<Int>,
    playlists: List<Playlist> = emptyList(),
    playerState: PlayerState,
    getPlaylistItems: (String) -> Flow<List<PlaylistItem>> = { kotlinx.coroutines.flow.emptyFlow() },
    onCreatePlaylist: (name: String, description: String, colorHex: String) -> Unit = { _, _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onPlayPlaylist: (playlist: Playlist, startIndex: Int, shuffle: Boolean) -> Unit = { _, _, _ -> },
    onRemovePlaylistItem: (String) -> Unit = {},
    onReciterClick: (Reciter) -> Unit,
    onQuickPlayReciter: (Reciter) -> Unit,
    onToggleFavoriteReciter: (String) -> Unit,
    onSurahClick: (Surah) -> Unit,
    onToggleFavoriteSurah: (Int) -> Unit,
    onExploreClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "السور المفضلة (${favoriteSurahs.size})",
        "القراء المفضلون (${favoriteReciters.size})",
        "قوائم التشغيل (${playlists.size})"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IslamicGeometricBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // Favorite Surahs Tab
                if (favoriteSurahs.isEmpty()) {
                    EmptyFavoritesState(
                        title = "لم تضف أي سورة للمفضلة بعد",
                        subtitle = "اضغط على رمز القلب بجوار أي سورة لإضافتها إلى قائمتك المفضلة وسرعة الوصول إليها",
                        onExplore = onExploreClicked
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteSurahs, key = { it.number }) { surah ->
                            val isPlaying = playerState.currentItem?.surahNumber == surah.number
                            SurahListItemCard(
                                surah = surah,
                                isPlaying = isPlaying,
                                isFavorite = true,
                                onClick = { onSurahClick(surah) },
                                onToggleFavorite = { onToggleFavoriteSurah(surah.number) }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Favorite Reciters Tab
                if (favoriteReciters.isEmpty()) {
                    EmptyFavoritesState(
                        title = "لم تضف أي قارئ للمفضلة بعد",
                        subtitle = "اضغط على رمز النجمة بجوار أي قارئ للاستماع السريع لتلاواته في أي وقت",
                        onExplore = onExploreClicked
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoriteReciters, key = { it.id }) { reciter ->
                            val isCurrentlyPlaying = playerState.currentItem?.reciterId == reciter.id
                            ReciterItemCard(
                                reciter = reciter,
                                isFavorite = true,
                                isCurrentlyPlaying = isCurrentlyPlaying,
                                onReciterClick = { onReciterClick(reciter) },
                                onToggleFavorite = { onToggleFavoriteReciter(reciter.id) },
                                onQuickPlay = { onQuickPlayReciter(reciter) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Playlists Tab
                PlaylistsScreen(
                    playlists = playlists,
                    playerState = playerState,
                    getPlaylistItems = getPlaylistItems,
                    onCreatePlaylist = onCreatePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onPlayPlaylist = onPlayPlaylist,
                    onRemovePlaylistItem = onRemovePlaylistItem
                )
            }
        }
    }
}
}

@Composable
fun EmptyFavoritesState(
    title: String,
    subtitle: String,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onExplore,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_explore_favorites")
            ) {
                Icon(imageVector = Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "استكشف السور والقراء", fontWeight = FontWeight.Bold)
            }
        }
    }
}
