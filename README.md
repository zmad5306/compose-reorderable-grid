# Compose Reorderable Grid

> **Silky‑smooth drag‑and‑drop grids for Jetpack Compose.**
>
> Built for real apps. Tuned through battle. Polished for publishing.

---

## ✨ What This Is

`compose-reorderable-grid` is a **high‑performance, long‑press drag‑and‑drop grid** for Jetpack Compose.

It solves the hard parts Compose doesn’t give you out of the box:

* Natural card displacement while dragging
* Predictable drop targets
* Smooth auto‑scroll while holding an item
* Stable gestures that *don’t* cancel mid‑drag
* Works with large lists and real scrolling containers

This isn’t a demo toy.
It’s extracted from a production app, stress‑tested on physical devices, and cleaned up so future‑you won’t hate present‑you.

---

## 🧠 Design Philosophy

This library is opinionated — intentionally.

**Principles:**

* **Long‑press starts the drag** (muscle memory matters)
* **Other items move out of the way immediately** (no overlap jank)
* **Scrolling while dragging must feel fast** (no edge‑hugging required)
* **No pointerInput restarts mid‑gesture** (ever)
* **Layout math is explicit and debuggable**

Under the hood, it uses:

* `pointerInput(Unit)` + `rememberUpdatedState` to keep gestures alive
* Frozen item heights to prevent grid reflow chaos
* Padding‑aware, center‑based hit testing for predictable drops
* Non‑state layout caches to avoid recomposition storms

Every line exists because something broke without it.

---

## 🖐️ Interaction Model

* **Long‑press** a card to lift it
* Drag freely across rows and columns
* Scroll the grid *while holding the card*
* Release to drop — indices update cleanly

No flicker. No snapping back. No “why did it drop there?” moments.

---

## 🚀 Usage (High‑Level)

```kotlin
ReorderableLazyVerticalGrid(
    items = items,
    columns = 2,
    key = { it.id },
    onMove = { from, to ->
        items = items.move(from, to)
    }
) { item, isDragging ->
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = if (isDragging) 8.dp else 2.dp
    ) {
        /* content */
    }
}
```

That’s it.
No adapters. No controllers. No ceremony.

---

## 🧪 Why This Exists

Compose gives you animation primitives.
It does **not** give you a reorderable grid that:

* scrolls correctly
* animates consistently
* survives recomposition
* behaves on real devices

This repo exists because shipping apps exposed edge cases that blog‑post examples never hit.

The code you see here is what was left *after* removing everything that caused twitching, merging, snapping, or gesture cancellation.

---

## 🧹 Code Quality Guarantees

* No duplicated state sources
* No captured stale lambdas
* No accidental pointer restarts
* No unnecessary Compose state

The file is intentionally **boring** now.
That’s how you know it’s ready.

---

## 📦 Status

* ✅ Actively used
* ✅ Safe to vendor
* 🚧 Publishing to Maven Central planned

API is small on purpose. Expect additive changes only.

---

## ❤️ Credits

Created by **Zach Maddox**

Built while shipping a real product.
Refined by breaking it repeatedly.

If this saves you a weekend — or prevents one late‑night rage‑refactor — it’s done its job.

---

> *Smooth is not optional.*
> *Predictable is not negotiable.*
> *Jank is a bug.*
