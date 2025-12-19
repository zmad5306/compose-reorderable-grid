package dev.zachmaddox.compose.reorderable.grid.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.zachmaddox.compose.reorderable.grid.ReorderableLazyVerticalGrid
import dev.zachmaddox.compose.reorderable.grid.rememberReorderableLazyGridState
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

@Composable
private fun SampleApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ReorderableGridDemo()
        }
    }
}

@Immutable
private data class DemoCard(
    val id: String,
    val label: String,
    val orderIndex: Int
)

@Composable
private fun ReorderableGridDemo() {
    val initial = remember {
        List(48) { i ->
            DemoCard(
                id = UUID.randomUUID().toString(),
                label = "Card ${i + 1}",
                orderIndex = i
            )
        }
    }

    val cards = remember { mutableStateListOf<DemoCard>().apply { addAll(initial) } }

    fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val ordered = cards.sortedBy { it.orderIndex }.toMutableList()
        val moved = ordered.removeAt(fromIndex)
        val safeTo = toIndex.coerceIn(0, ordered.size)
        ordered.add(safeTo, moved)

        val reindexed = ordered.mapIndexed { idx, card ->
            card.copy(orderIndex = idx)
        }

        cards.clear()
        cards.addAll(reindexed)
    }

    val state = rememberReorderableLazyGridState(onMove = ::reorder)
    val ordered = cards.sortedBy { it.orderIndex }

    ReorderableLazyVerticalGrid(
        items = ordered,
        key = { it.id },
        state = state,
        columns = 3,
        contentPadding = PaddingValues(16.dp),
        horizontalSpacing = 12.dp,
        verticalSpacing = 12.dp
    ) { card, isDragging ->
        DemoGridCard(card, isDragging)
    }
}

@Composable
private fun DemoGridCard(
    card: DemoCard,
    isDragging: Boolean
) {
    val scale = if (isDragging) 1.03f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .scale(scale),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
