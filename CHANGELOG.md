# 🧾 Changelog

All notable changes to **Compose Reorderable Gird** will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2025-12-18

### Upgraded

- Dependencies and plugins

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
