package iptv.rokid.max.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.tv.material3.*
import iptv.rokid.max.data.Category
import iptv.rokid.max.data.Channel
import iptv.rokid.max.data.M3uParser
import iptv.rokid.max.data.RecentChannelManager
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen(onChannelSelected: (Channel) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val channelsListState = rememberLazyListState()
    val firstChannelFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val nestedScrollConnection = remember(selectedCategoryIndex, categories.size) {
        var overscrollX = 0f
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag) {
                    if (available.x < 0) {
                        overscrollX += available.x
                        if (overscrollX < -150f && selectedCategoryIndex < categories.size - 1) {
                            selectedCategoryIndex++
                            overscrollX = 0f
                        }
                    } else if (available.x > 0) {
                        overscrollX += available.x
                        if (overscrollX > 150f && selectedCategoryIndex > 0) {
                            selectedCategoryIndex--
                            overscrollX = 0f
                        }
                    } else {
                        overscrollX = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(selectedCategoryIndex) {
        channelsListState.scrollToItem(0)
    }

    LaunchedEffect(Unit) {
        isLoading = true
        categories = M3uParser.fetchAndParse(context)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(text = "Đang tải dữ liệu M3U...", color = Color.White, fontSize = 24.sp)
        }
        return
    }

    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(text = "Không tìm thấy kênh nào.", color = Color.White, fontSize = 24.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        // TabRow for Categories
        LazyRow(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .focusProperties {
                    down = firstChannelFocusRequester
                },
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                CategoryTab(
                    title = category.name,
                    isSelected = index == selectedCategoryIndex,
                    onClick = { selectedCategoryIndex = index }
                )
            }
        }

        // Horizontal List of Channels matching selected category
        val selectedCategory = categories.getOrNull(selectedCategoryIndex)
        if (selectedCategory != null) {
            LazyRow(
                state = channelsListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(selectedCategory.channels) { index, channel ->
                    val isFirstItem = index == 0
                    val isLastItem = index == selectedCategory.channels.size - 1
                    
                    val itemModifier = Modifier
                        .then(if (isFirstItem) Modifier.focusRequester(firstChannelFocusRequester) else Modifier)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (isLastItem && event.key == Key.DirectionRight) {
                                    if (selectedCategoryIndex < categories.size - 1) {
                                        selectedCategoryIndex++
                                        return@onKeyEvent true
                                    }
                                }
                                if (isFirstItem && event.key == Key.DirectionLeft) {
                                    if (selectedCategoryIndex > 0) {
                                        selectedCategoryIndex--
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        }
                        
                    ChannelCard(
                        channel = channel,
                        modifier = itemModifier,
                        onClick = { 
                            RecentChannelManager.saveRecentChannel(context, channel)
                            onChannelSelected(channel) 
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color.White else Color(0xFF111111),
            contentColor = if (isSelected) Color.Black else Color.LightGray,
            focusedContainerColor = Color(0xFFE50914), // Netflix Red for focus
            focusedContentColor = Color.White
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelCard(channel: Channel, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.name,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}
