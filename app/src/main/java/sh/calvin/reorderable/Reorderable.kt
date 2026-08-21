package sh.calvin.reorderable

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface LazyListItemInfo {
    val index: Int
}

class ReorderableLazyListState {
    val isAnyItemDragging: Boolean = false
    val isDragging: Boolean = false
}

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: (from: LazyListItemInfo, to: LazyListItemInfo) -> Unit
): ReorderableLazyListState {
    return ReorderableLazyListState()
}

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: () -> Unit = {}
): ReorderableLazyListState {
    return ReorderableLazyListState()
}

interface ReorderableItemScope {
    fun Modifier.draggableHandle(
        onDragStarted: () -> Unit = {},
        onDragStopped: () -> Unit = {}
    ): Modifier = this
}

private val DummyScope = object : ReorderableItemScope {}

@Composable
fun ReorderableItem(
    reorderableState: ReorderableLazyListState,
    key: Any?,
    content: @Composable ReorderableItemScope.(isDragging: Boolean) -> Unit
) {
    DummyScope.content(false)
}

// Fallback extension just in case it's used outside
fun Modifier.draggableHandle(
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {}
): Modifier = this
