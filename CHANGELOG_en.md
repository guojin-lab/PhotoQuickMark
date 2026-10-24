# Changelog

All notable changes to PhotoQuickMark. Version numbers match `versionName` / `versionCode` in `app/build.gradle.kts`.

## 1.0.1 — 2026-10-24

versionCode 5. Install the new APK over the previous build; the database migrates from v1 to v2 automatically and existing projects, photos and annotations are kept.

### Added

- **Rotate the photo 90° left / right**: the entry moved from the top-right overflow menu into the editor's bottom toolbar, after Undo and Redo, using Material Icons `Rotate90DegreesCcw` / `Rotate90DegreesCw`. The two rotation items were removed from the menu.
- Rotation transforms every annotation vertex, keeping segment ids, measured values, units and pen styles unchanged. The canvas swaps width and height on odd turns, and labels recompute their angle so they stay upright and readable.
- Rotation and annotation edits share one undo entry: one undo returns both the photo and its annotations to the pre-rotation state, and redo does the reverse.
- The rotation is persisted in the local database (`Photo.rotation`, 0–3 for 0°/90°/180°/270° clockwise) and restored when the project is reopened.

### Fixed

- Dimensions on some angles printed upside down. Label angles are now folded by half turns into (-90°, 90°], so reversing a line's endpoints no longer flips its label.

### Changed

- **App icon**: replaced with a new adaptive icon (photo frame, mountains, dimension arrows and a 0–40 ruler), with the foreground content inside the 72dp safe zone; all density PNGs were regenerated.
- **Database v1 → v2**: adds a `rotation` column (default 0); the migration runs automatically at startup.
- Image export, batch PDF and list thumbnails now decode with the saved rotation. Original photo files are never rewritten.

### Verified

- 36 JVM unit tests and 11 device tests pass, covering rotation maths, undo/redo, label angles, bitmap rotation, the database migration and canvas interaction.
- The full UI flow test (`EditorFlowTest`) stalls on Compose idle-waiting on this MIUI build and did not complete, so it is not counted.
- Real-device upgrade (22127RK46C / Android 15): the migration succeeded and the existing project with 3 annotated photos was preserved.

Changes in 1.0.0 and earlier are listed on the [Releases](https://github.com/guojin-lab/PhotoQuickMark/releases) page and in the git history.
