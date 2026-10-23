# Design System: Giri Precision POS & Telemetry (Dark Obsidian Studio)

## 1. Visual Atmosphere & Philosophy
A high-precision, tactical dark workspace interface designed for fast-paced retail and print operations. The aesthetic fuses mid-century industrial utility, Awwwards-tier dark obsidian depth, and double-bezel machined hardware architecture.

- **Density:** 6 (Balanced Telemetry & POS Ergonomics)
- **Variance:** 8 (Asymmetrical Bento Grids & Specialized Module Zones)
- **Motion:** 4 (Restrained Tactile Feedback & Micro-Transitions)

## 2. Color Calibration & Semantic Roles
- **Canvas Obsidian:** `#0A0C10` — Core application root background
- **Surface Machine:** `#12161D` — Primary card & table container surface
- **Surface Elevated:** `#1A202A` — Inputs, floating panels, active states, receipt container
- **Surface Subtle:** `#222A36` — Hover states, button backdrops
- **Border Hairline:** `#2A3442` — 1px structural dividers and nested double-bezels
- **Border Focus:** `#F59E0B` — Active control focus ring
- **Text Primary (Pure Ink):** `#F1F5F9` — High contrast display and body text
- **Text Secondary (Steel):** `#94A3B8` — Metadata, unit labels, subtitles
- **Text Faint:** `#64748B` — Table column headers, placeholders
- **Accent Solar Amber:** `#F59E0B` / `#D97706` — Primary CTA buttons, active navigation, key figures
- **Status Green (Success / Money):** `#10B981` (BG: `#064E3B`, FG: `#34D399`)
- **Status Amber (Waiting / Menipis):** `#F59E0B` (BG: `#78350F`, FG: `#FBBF24`)
- **Status Blue (Process / Service):** `#3B82F6` (BG: `#1E3A8A`, FG: `#60A5FA`)
- **Status Danger (Habis / Error):** `#EF4444` (BG: `#7F1D1D`, FG: `#F87171`)
- **Status Neutral (Finished / Picked):** `#64748B` (BG: `#1E293B`, FG: `#94A3B8`)

## 3. Typography Architecture
- **Display / Headers:** `Segoe UI Variable Display` / `Segoe UI Semibold` / `SansSerif`, tracking tight, bold architectural weight.
- **Body:** `Segoe UI Variable Text` / `Segoe UI` / `SansSerif` (12–14px).
- **Numbers & Telemetry:** `Cascadia Mono` / `Consolas` / `Monospaced` for all currency (`Rp ...`), quantities, invoice codes, and timers.

## 4. Component Stylings
- **Double-Bezel Cards (`UiKit.SurfacePanel`):** Rendered with a solid outer border `#2A3442`, rich inner fill `#12161D`, 8px corner radius, and subtle 1px top highlight.
- **Buttons (`UiKit.SoftButton`):** Tactile push shift on press, crisp border, high-contrast labels.
- **Data Tables (`UiKit.AppTable`):** 46px-52px row height, alternate row highlights on hover, distinct monospace columns.
- **Badges (`UiKit.BadgeLabel`):** Subtle tinted background with matching high-contrast text, 6px radius.
- **Inputs & Fields:** Deep recessed background `#1A202A`, 1px border `#2A3442`, focus ring in Solar Amber `#F59E0B`.
