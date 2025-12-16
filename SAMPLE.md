# Sample Project & Playground

This repository includes a **fully working sample app** you can use to explore, stress‑test, and understand the reorderable grid behavior before integrating it into your own project.

If you’re evaluating whether this library fits your use case, **start here**.

---

## 🧪 What the Sample App Demonstrates

The sample project is intentionally small, focused, and brutally honest. It shows:

* Long‑press to start drag (no custom gestures required)
* Smooth reordering across rows and columns
* Auto‑scrolling while holding an item
* Correct displacement of surrounding items
* Stable behavior during fast drags, edge scrolling, and rapid reorders

There are no hidden helpers or magic adapters — the sample uses the **exact same public API** you’ll use in your app.

---

## 🗂 Project Structure

At a high level:

```
root/
├─ app/                  # Sample Android app
│  ├─ MainActivity.kt    # Reorderable grid demo
│  └─ ui/                # Simple card UI
├─ reorderable-grid/     # Library module
│  └─ ReorderableGrid.kt # Core implementation
```

You can:

* Copy the library module directly
* Run the app and experiment with behavior
* Modify item sizes, column counts, or scroll speed

Nothing is mocked. Nothing is stubbed.

---

## ▶️ Running the Sample

1. Open the project in **Android Studio**
2. Select the `app` run configuration
3. Deploy to:

   * an emulator **and**
   * at least one physical device (recommended)

Drag aggressively. Scroll hard. Try to break it.

That’s exactly how this code was hardened.

---

## 🔧 Things Worth Experimenting With

While running the sample, try:

* Increasing the item count significantly
* Switching between portrait and landscape
* Dragging items across multiple scroll lengths
* Changing column counts at runtime
* Adding visual affordances (borders, elevation, scaling)

The grid logic is designed to stay stable under all of these.

---

## 🧭 Why Keep This Separate

The sample app exists for **learning and validation**, not as required infrastructure.

Once you’re comfortable:

* Delete the `app/` module
* Keep only the library module
* Or vendor the single source file directly

The library has **no dependency** on the sample.

---

## 🧠 Recommendation

If you’re debugging behavior in your own app:

> Reproduce it in the sample first.

If it works there, the issue is almost always interaction with surrounding UI (nested scroll, window insets, decorations, etc.).

---

Happy dragging.
