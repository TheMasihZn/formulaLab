# formulaLab

> **Status: TODO, not built yet.**
> This README holds the prompt that will build the app. When Masih says
> "build formulaLab", run the prompt below in this repository.

Part of Masih's OS (Hearth). It starts as a standalone Android app for testing and becomes an OS feature later.

---

## Build prompt

Build **formulaLab**, an Android app in this repository (`TheMasihZn/formulaLab`), built to an APK by GitHub Actions. Every push to `main` publishes a numbered release, the same pipeline as `TheMasihZn/frameExtractor`: Gradle 8.9 with no wrapper, JDK 17, AGP 8.5.2, Kotlin 1.9.24, minSdk 29, targetSdk 34, package `com.themasihzn.formulalab`.

### What it does
1. **Describe → formula.** The user types or dictates a description, e.g. "the fridge air's temperature difference decays with a time constant". An LLM returns the formula written in **Masih's notation**.
2. **Render.** Show the LaTeX offline with KaTeX bundled in `assets/`, with no network needed for rendering.
3. **readMath.** Turn every formula into spoken text with a **deterministic rule-based converter**, not the LLM, so it reads the same way every time. Show the text and speak it with Android TextToSpeech.
4. **Calculate.** Evaluate the formula with math.js (bundled), using an input field per variable, show the result, and plot the result over one chosen variable (e.g. t) on a canvas.
5. **History.** Save every formula as a `.formula` file: a zip holding `<name>.tex` (LaTeX), `<name>.txt` (readMath text) and `<name>.json` (variables, values, glossary, description). Keep it in app storage and mirror it to `Documents/formulaLab/` through MediaStore. Show a history list, and tap an item to reopen it.
6. **Manual mode.** Typing or pasting LaTeX directly also works without the AI.

### Masih's notation (the LLM must follow it, and readMath must understand it)
- **One derivative/difference symbol: `\partial`.** Never use `\Delta`, `d/dt`, `\frac{d}{dt}` or a dot on top.
  - `\partial_t X`: change of X over time.
  - `\partial_{\mathrm{air}} T_{\mathrm{air}}`: the index under ∂ repeats the quantity's own index, so this is a **plain difference** (what Δ used to mean). The reference, e.g. the room, stays implied, as it was with Δ.
- **Subscripts naming things are full words** in `\mathrm{}`: `T_{\mathrm{air}}`, `\tau_{\mathrm{food}}`.
- **Einstein summation convention:** a repeated upper/lower index is summed, e.g. `\partial_t\, \partial T^{i} = -K^{i}{}_{j}\, \partial T^{j}`.
- One capital letter per quantity.

Reference example (the fridge):
```latex
\partial_t\, \partial_{\mathrm{air}} T_{\mathrm{air}} = -\frac{\partial_{\mathrm{air}} T_{\mathrm{air}}}{\tau_{\mathrm{air}}}
```

### readMath rules (read in written order, each symbol as its word)
- `\partial_t X` → "the differences over time of X"
- `\partial_{w} X_{w}` (repeated index) → "the difference of X"
- `X_{\mathrm{word}}` → "X of the word", using a glossary (e.g. T → "temperature", t → "time")
- `\frac{a}{b}` → "a over b"; `e^{x}` → "e to the x"; `x^{2}` → "x squared"
- Single-letter index: `^{i}` → "upper i", `_{j}` → "lower j"
- Greek letters by name ("tau", "theta"); `=` → "equals"; leading `-` → "negative", otherwise "minus"; `\approx` → "approximately equals"
- `X(t)` → "X at t"
- Expected output for the fridge example: *"the differences over time of the difference of temperature of the air equals the negative difference of temperature of the air over the tau of the air"*
- readMath also applies to maths inside ordinary sentences.

### LLM
- Default: **Gemini API** with the user's own key, entered in settings and kept on the device. The model name is configurable, default `gemini-flash-latest`. Make the HTTP call from Kotlin, not the WebView, and ask for JSON output.
- JSON returned: `title`, `latex` (lines), `glossary` (symbol → meaning), `expression` (math.js syntax, ASCII variable names), `variables` (name, meaning, default value, unit), `plotVar`.
- Later: swap to **local Gemma** on the device. Keep the LLM behind one interface so it's replaceable (modularity rule).

### Interfaces (OS rules)
- **Touch / mouse:** a compact layout that fits the **smallest multi-window / pop-up size**, with a scrollable column.
- **Keyboard:** **hold Ctrl and type any label** visible on screen (button, field, placeholder). The match is highlighted, and releasing Ctrl or pressing Enter presses it. Esc cancels, Backspace edits. In text fields, Ctrl+C/V/X/A/Z still work.
- **API for the AI and other features:** intent actions `com.themasihzn.formulalab.GENERATE` (extra: description), `RENDER` (extra: latex) and `READ` (extra: latex → returns/speaks readMath).
- **Share in:** share text into the app to turn it into a formula.

### Look
- OLED style: black background, outline drawings, teal `#4FD1C5` and amber `#F6C453` accents (same as frameExtractor).
- Adaptive vector icon: an outlined ∂ with two amber lines and a check mark. The SVG source goes in `design/icon.svg`.

### Done when
- The APK builds on push and installs on a Galaxy Z Fold 5.
- The fridge example renders, reads aloud exactly as above, calculates, plots over t, and saves and reopens as a `.formula` file.
