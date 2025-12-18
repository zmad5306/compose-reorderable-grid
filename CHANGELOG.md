# 🧾 Changelog

All notable changes to **Compose Reorderable Gird** will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.2] - 2025-12-17

### Fixed

- Fixed an issue where dragging an item past the fold or to the bottom of a long, scrollable grid and then dragging upward could cause the grid to snap back to the top.
- Corrected scroll anchor handling to ensure top-pinning logic is disabled once the grid scrolls away from the top during an active drag.
- Improved auto-scroll and anchor reconciliation to prevent unexpected scroll jumps during long-distance drag operations.
- Further hardened drag state management for large, scrollable grids.

---

## [1.0.1] - 2025-12-17

### Fixed

- Fixed an edge-case drag-and-drop bug when the grid height is smaller than the viewport (non-scrollable grids).
- Resolved an issue where dragging an item beyond the end of a short grid and back upward could cause the dragged item to visually “stick” on top of another item instead of properly displacing it.
- Corrected internal index tracking to ensure the dragged item’s source index is always derived from the current item list by key, preventing invalid indices (e.g. `items.size`) from being used during reordering.
- Improved robustness of drag state reconciliation when temporarily targeting the end insertion slot.

---

## [1.0.0] - 2025-12-16

### Added

- Initial stable release of `compose-reorderable-grid`
- Long-press drag-and-drop reordering for `LazyVerticalGrid`
- Smooth, animated item displacement during drag
- Auto-scrolling support while dragging items beyond the visible viewport
- Stable key-based reordering to prevent item flicker or state loss
- Padding-aware hit testing and center-based drop detection
- Support for dynamic item size changes (e.g., conditional borders, winner highlights)
- Public, reusable API designed for library consumption

### Stability

- Hardened gesture handling to prevent item merging, twitching, or layout corruption
- Eliminated layout instability caused by modifier-driven size changes
- Consistent internal state management using `rememberUpdatedState`

### Documentation

- Initial README with usage examples and integration guidance
- Clear API documentation focused on correct Compose usage patterns

---
