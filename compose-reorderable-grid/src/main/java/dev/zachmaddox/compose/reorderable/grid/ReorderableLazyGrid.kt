package dev.zachmaddox.compose.reorderable.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.hypot

/**
 * Remembers drag state for [ReorderableLazyVerticalGrid]. You must provide [onMove] to reorder
 * your backing list when a drag crosses an item boundary.
 */
@Composable
fun rememberReorderableLazyGridState(
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState(),
    hapticFeedbackEnabled: Boolean = true
): ReorderableLazyGridState {
    val scope = rememberCoroutineScope()
    return remember(gridState, onMove, hapticFeedbackEnabled, scope) {
        ReorderableLazyGridState(
            gridState = gridState,
            onMove = onMove,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            scope = scope
        )
    }
}

/**
 * Stateful long-press-drag-to-reorder grid built on [LazyVerticalGrid].
 *
 * Consumers must keep [items] ordered and stable by key; the [ReorderableLazyGridState] created by
 * [rememberReorderableLazyGridState] provides the move callback that should reorder your backing list
 * so the next composition reflects the new ordering.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderableLazyVerticalGrid(
    items: List<T>,
    key: (T) -> Any,
    state: ReorderableLazyGridState,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    endSlot: (@Composable (highlighted: Boolean) -> Unit)? = { highlighted ->
        DefaultEndSlot(
            highlighted = highlighted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    },
    itemContent: @Composable LazyGridItemScope.(item: T, isDragging: Boolean) -> Unit
) {
    require(columns > 0) { "columns must be greater than zero" }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val haptics = LocalHapticFeedback.current.takeIf { state.hapticFeedbackEnabled }
    val touchSlopPx = LocalViewConfiguration.current.touchSlop

    val horizontalSpacingPx = with(density) { horizontalSpacing.toPx() }
    val currentColumns by rememberUpdatedState(columns)
    val verticalSpacingPx = with(density) { verticalSpacing.toPx() }

    // IMPORTANT: pointerInput must NOT restart when draggingKey changes or when the list reorders.
    val currentItems by rememberUpdatedState(items)
    val currentKey by rememberUpdatedState(key)

    val draggedZ = 10f

    fun findIndexByKey(keyValue: Any): Int =
        currentItems.indexOfFirst { currentKey(it) == keyValue }

    fun computeToIndex(pointerInRoot: Offset, itemCount: Int): Int {
        // "Put it where my finger is" — interpreted as the GRID CELL (slot) under the finger.
        // DO NOT target "which item" under the finger, because reorders/animations move items
        // under a stationary finger and cause target oscillation ("fighting").

        val grid = state.gridBoundsInRoot ?: return -1

        val padLeftPx = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx() }
        val padRightPx = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx() }
        val padTopPx = with(density) { contentPadding.calculateTopPadding().toPx() }

        val cols = currentColumns.coerceAtLeast(1)

        val gridContentWidthPx = (grid.width - padLeftPx - padRightPx)
        val cellWidthPx = ((gridContentWidthPx - horizontalSpacingPx * (cols - 1)) / cols)
            .coerceAtLeast(1f)

        val cellHeightPx = (
                if (state.draggingKey != null && state.frozenCellHeightPx > 0f) state.frozenCellHeightPx
                else state.cellHeightPx
                ).coerceAtLeast(1f)

        state.cellWidthPx = cellWidthPx

        val localX = (pointerInRoot.x - grid.left) - padLeftPx
        val localY = (pointerInRoot.y - grid.top) - padTopPx

        val stepX = cellWidthPx + horizontalSpacingPx
        val stepY = cellHeightPx + verticalSpacingPx

        val col = floor(localX / stepX).toInt().coerceIn(0, cols - 1)

        // Account for scroll position so the finger maps to the correct slot off-screen.
        // We anchor the row calculation to the first visible row, but intentionally DO NOT add
        // `firstVisibleItemScrollOffset` here. During a drag, that offset can change abruptly
        // (e.g., when a fling is being stopped), which can cause the target row to jump "a page".
        // Using only the discrete first visible row produces stable targets and smooth displacement.
        val firstIndex = state.gridState.firstVisibleItemIndex
        val firstRow = if (firstIndex <= 0) 0 else (firstIndex / cols)

        val rowInViewport = floor(localY / stepY).toInt()
        val row = (rowInViewport + firstRow).coerceAtLeast(0)

        val index = (row * cols + col).coerceIn(0, itemCount)
        return index
    }

    DisposableEffect(Unit) {
        onDispose { state.autoScrollJob?.cancel() }
    }

    fun updateDraggedTranslation() {
        val dragKey = state.draggingKey ?: return
        val rect = state.itemBoundsInRoot[dragKey] ?: return
        val desiredTopLeft = state.pointerInRoot - state.grabOffsetInItem
        state.dragTranslation = desiredTopLeft - rect.topLeft
    }

    fun resetDragState() {
        state.autoScrollJob?.cancel()
        state.autoScrollJob = null
        state.autoScrollDeltaPx = 0f

        state.draggingKey = null
        state.draggingIndex = -1

        state.pendingKey = null
        state.pendingPointerStartInRoot = Offset.Zero
        state.pendingGrabOffsetInItem = Offset.Zero

        state.scrollAnchorKey = null
        state.scrollAnchorOffsetPx = 0

        state.dragTranslation = Offset.Zero
        state.grabOffsetInItem = Offset.Zero
        state.pointerInRoot = Offset.Zero
        state.currentToIndex = -1

        state.frozenCellHeightPx = 0f
    }

    fun activatePendingDragIfAny() {
        val keyToDrag = state.pendingKey ?: return

        state.draggingKey = keyToDrag
        state.draggingIndex = findIndexByKey(keyToDrag)
        state.grabOffsetInItem = state.pendingGrabOffsetInItem

        // Disable any scroll fling that might be in progress before we start auto-scrolling.
        state.scope.launch { state.gridState.stopScroll() }

        // NOTE: We intentionally do NOT "pin" the scroll position during drag.
        // Pinning (scrollToItem corrections) can interfere with animateItem() placement animations,
        // especially when moving the top-left (anchor) item away and back.
        state.scrollAnchorKey = null
        state.scrollAnchorOffsetPx = 0

        state.pendingKey = null
        state.pendingPointerStartInRoot = Offset.Zero
        state.pendingGrabOffsetInItem = Offset.Zero

        updateDraggedTranslation()
    }

    fun startAutoScrollIfNeeded(pointerInRoot: Offset) {
        val grid = state.gridBoundsInRoot ?: return
        if (state.draggingKey == null) return

        // Start auto-scrolling when the pointer approaches the top/bottom edges of the grid.
        val edgePx = with(density) { 56.dp.toPx() }
        val maxSpeedPx = with(density) { 22.dp.toPx() }

        val distanceToTop = (pointerInRoot.y - grid.top).coerceAtLeast(0f)
        val distanceToBottom = (grid.bottom - pointerInRoot.y).coerceAtLeast(0f)

        val delta = when {
            distanceToTop < edgePx -> {
                val t = (1f - (distanceToTop / edgePx)).coerceIn(0f, 1f)
                -maxSpeedPx * t
            }
            distanceToBottom < edgePx -> {
                val t = (1f - (distanceToBottom / edgePx)).coerceIn(0f, 1f)
                maxSpeedPx * t
            }
            else -> 0f
        }

        state.autoScrollDeltaPx = delta

        if (delta == 0f) {
            state.autoScrollJob?.cancel()
            state.autoScrollJob = null
            return
        }

        if (state.autoScrollJob?.isActive == true) return

        state.autoScrollJob = state.scope.launch {
            while (true) {
                val d = state.autoScrollDeltaPx
                if (d == 0f) break

                state.gridState.scrollBy(d)

                // Allow layout/measure to catch up so hit-testing/placement stays stable.
                delay(16)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                state.gridBoundsInRoot = coords.boundsInRoot()
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localDown ->
                        val grid = state.gridBoundsInRoot ?: return@detectDragGesturesAfterLongPress
                        val downInRoot = grid.topLeft + localDown

                        val hitKey = state.itemBoundsInRoot.entries
                            .firstOrNull { (_, r) -> r.contains(downInRoot) }
                            ?.key
                            ?: return@detectDragGesturesAfterLongPress

                        val hitRect = state.itemBoundsInRoot[hitKey] ?: return@detectDragGesturesAfterLongPress

                        // Freeze the cell height based on the actual hit item so we never start with 0px
                        // (which would make stepY ~ 1px and cause huge target index jumps).
                        val hitHeightPx = hitRect.height
                        if (hitHeightPx > 0f) {
                            state.frozenCellHeightPx = hitHeightPx
                            if (state.cellHeightPx <= 0f) state.cellHeightPx = hitHeightPx
                        }

                        state.pendingKey = hitKey
                        state.pendingPointerStartInRoot = downInRoot
                        state.pendingGrabOffsetInItem = downInRoot - hitRect.topLeft

                        state.pointerInRoot = downInRoot
                        state.dragTranslation = Offset.Zero
                        state.currentToIndex = findIndexByKey(hitKey)
                    },
                    onDragCancel = { resetDragState() },
                    onDragEnd = { resetDragState() },
                    onDrag = { change, _ ->
                        // IMPORTANT: Don't accumulate `positionChange()` deltas.
                        // When items reorder or the grid scrolls, local coordinates can shift, and
                        // `positionChange()` may include layout-driven movement. That can make the
                        // stored pointer jump (especially when dragging the top-left anchor item).
                        // Instead, re-derive the pointer in root space from the current pointer position.
                        val grid = state.gridBoundsInRoot ?: return@detectDragGesturesAfterLongPress
                        val prevPointer = state.pointerInRoot
                        state.pointerInRoot = grid.topLeft + change.position
                        val delta = state.pointerInRoot - prevPointer

                        if (state.draggingKey == null) {
                            val pending = state.pendingKey
                            val start = state.pendingPointerStartInRoot
                            if (pending == null || start == Offset.Zero) return@detectDragGesturesAfterLongPress

                            val d = state.pointerInRoot - start
                            val dist = hypot(d.x, d.y)
                            if (dist < touchSlopPx) return@detectDragGesturesAfterLongPress

                            activatePendingDragIfAny()
                            change.consume()
                        } else {
                            if (delta != Offset.Zero) change.consume()
                        }

                        val dragKey = state.draggingKey ?: return@detectDragGesturesAfterLongPress

                        // IMPORTANT: Always derive the "from" index from the current list.
                        // When dragging past the end we may temporarily target `toIndex == items.size`,
                        // but the dragged item's actual index is still 0..lastIndex.
                        val indexNow = findIndexByKey(dragKey)
                        if (indexNow < 0) return@detectDragGesturesAfterLongPress
                        state.draggingIndex = indexNow

                        updateDraggedTranslation()
                        startAutoScrollIfNeeded(state.pointerInRoot)

                        // (Scroll pinning disabled)

                        // Target selection is based on the finger location, not the dragged card center.
                        val proposedRaw = computeToIndex(state.pointerInRoot, currentItems.size)
                        if (proposedRaw < 0) return@detectDragGesturesAfterLongPress

                        // Prevent "page jumps" from transient geometry/scroll jitter by limiting how far
                        // the target index can move per pointer event. This keeps displacement animation
                        // smooth while still allowing long-distance moves over multiple frames.
                        val lastTarget = state.currentToIndex.takeIf { it >= 0 } ?: indexNow
                        val maxStep = currentColumns.coerceAtLeast(1) // 1 row
                        val proposed = proposedRaw.coerceIn(
                            (lastTarget - maxStep).coerceAtLeast(0),
                            (lastTarget + maxStep).coerceAtMost(currentItems.size)
                        )

                        val adjusted =
                            if (proposed > indexNow) proposed.coerceAtMost(currentItems.size) else proposed

                        var anchorIndex = 0
                        var anchorOffset = 0
                        var shouldReanchor = false
                        if (adjusted != indexNow) {
                            val firstVisibleKey = state.gridState.layoutInfo.visibleItemsInfo
                                .firstOrNull()
                                ?.key
                            if (firstVisibleKey == dragKey) {
                                anchorIndex = state.gridState.firstVisibleItemIndex
                                anchorOffset = state.gridState.firstVisibleItemScrollOffset
                                shouldReanchor = true
                            }
                        }

                        if (adjusted != state.currentToIndex) {
                            state.currentToIndex = adjusted
                            haptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        if (adjusted != indexNow) {
                            state.onMove(indexNow, adjusted)

                            if (shouldReanchor) {
                                state.scope.launch {
                                    state.gridState.scrollToItem(anchorIndex, anchorOffset)
                                }
                            }

                            // After the consumer reorders, re-derive the dragged index by key to avoid off-by-one
                            // and "stuck overlay" bugs when we previously targeted the end slot.
                            val newIndex = findIndexByKey(dragKey)
                            if (newIndex >= 0) state.draggingIndex = newIndex

                            // (Scroll pinning disabled)
                        }
                    }
                )
            }
    ) {
        val endSlotHighlighted = state.draggingKey != null && state.currentToIndex == currentItems.size

        LazyVerticalGrid(
            state = state.gridState,
            columns = GridCells.Fixed(currentColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            items(
                items = currentItems,
                key = { item -> currentKey(item) }
            ) { item ->
                val itemKey = currentKey(item)
                val isDragging = itemKey == state.draggingKey
                val translation = if (isDragging) state.dragTranslation else Offset.Zero

                Box(
                    modifier = Modifier
                        .animateItem()
                        .zIndex(if (isDragging) draggedZ else 0f)
                        .onGloballyPositioned { coords ->
                            state.itemBoundsInRoot[itemKey] = coords.boundsInRoot()
                            if (state.draggingKey == null) {
                                state.cellHeightPx = coords.size.height.toFloat()
                            }
                            if (isDragging) {
                                updateDraggedTranslation()
                            }
                        }
                        .graphicsLayerTranslation(translation, isDragging)
                ) {
                    // ✅ FIX: Call itemContent directly. We are already in LazyGridItemScope here.
                    itemContent(item, isDragging)
                }
            }

            if (endSlot != null) {
                item(
                    key = "end-slot",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    endSlot(endSlotHighlighted)
                }
            }
        }
    }
}

@Stable
class ReorderableLazyGridState internal constructor(
    val gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    internal val onMove: (Int, Int) -> Unit,
    internal val hapticFeedbackEnabled: Boolean,
    internal val scope: CoroutineScope
) {
    internal var draggingKey by mutableStateOf<Any?>(null)
    internal var draggingIndex by mutableIntStateOf(-1)

    // Touch-slop gated pending drag state.
    internal var pendingKey by mutableStateOf<Any?>(null)
    internal var pendingPointerStartInRoot by mutableStateOf(Offset.Zero)
    internal var pendingGrabOffsetInItem by mutableStateOf(Offset.Zero)

    internal var scrollAnchorKey by mutableStateOf<Any?>(null)
    internal var scrollAnchorOffsetPx by mutableIntStateOf(0)
    internal var dragTranslation by mutableStateOf(Offset.Zero)
    internal var grabOffsetInItem by mutableStateOf(Offset.Zero)
    internal var pointerInRoot by mutableStateOf(Offset.Zero)
    internal var currentToIndex by mutableIntStateOf(-1)
    internal var gridBoundsInRoot by mutableStateOf<Rect?>(null)
    internal var cellWidthPx by mutableFloatStateOf(0f)
    internal var cellHeightPx by mutableFloatStateOf(0f)
    internal var frozenCellHeightPx by mutableFloatStateOf(0f)

    internal val itemBoundsInRoot = mutableStateMapOf<Any, Rect>()
    internal var autoScrollJob by mutableStateOf<Job?>(null)
    internal var autoScrollDeltaPx by mutableFloatStateOf(0f)
}

private fun Modifier.graphicsLayerTranslation(translation: Offset, isDragging: Boolean): Modifier {
    if (!isDragging) return this
    return graphicsLayer(
        translationX = translation.x,
        translationY = translation.y
    )
}

@Composable
private fun DefaultEndSlot(
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val background =
        if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .sizeIn(minHeight = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (highlighted) "Release to drop at end" else "Drag here to drop at end",
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

