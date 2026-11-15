package com.nutriplanner.web.style;

import java.util.Map;

/**
 * Design tokens. The palette comes from the kitchen: spinach-green ink, jade for actions, and one food colour per
 * macronutrient that is used identically everywhere — beetroot for protein, turmeric for carbohydrate, blueberry for
 * fat and whole-grain brown for fibre.
 */
public final class Theme {

    private Theme() {
    }

    public static final String DISPLAY_FONT = "\"Bricolage Grotesque\", \"Inter\", ui-sans-serif, system-ui, sans-serif";
    public static final String TEXT_FONT = "\"Inter\", ui-sans-serif, system-ui, -apple-system, \"Segoe UI\", Roboto, sans-serif";

    public static final Map<String, String> LIGHT = StyleSheet.map(
            "bg", "#EEF3EF",
            "surface", "#FFFFFF",
            "surface-2", "#F6F9F6",
            "sunken", "#E4ECE6",
            "ink", "#16241F",
            "ink-2", "#3C4F46",
            "muted", "#5D6E66",
            "line", "#D8E2DA",
            "line-strong", "#BFCDC3",
            "jade", "#1D6B50",
            "jade-strong", "#14523D",
            "jade-soft", "#DDEFE5",
            "on-jade", "#FFFFFF",
            "turmeric", "#D9970F",
            "turmeric-soft", "#FBEFD2",
            "beet", "#A9346A",
            "beet-soft", "#F6E0EA",
            "blueberry", "#4459A6",
            "blueberry-soft", "#E2E6F6",
            "millet", "#8A6A3B",
            "millet-soft", "#F0E7D9",
            "chili", "#BF3B25",
            "chili-soft", "#F9E3DE",
            "veg", "#2E8540",
            "nonveg", "#9B3A1C",
            "egg", "#C98A00",
            "focus", "#D9970F",
            "shadow", "0 1px 2px rgba(22,36,31,.06), 0 8px 24px -12px rgba(22,36,31,.18)",
            "overlay", "rgba(16,28,23,.55)");

    public static final Map<String, String> DARK = StyleSheet.map(
            "bg", "#0E1613",
            "surface", "#15201B",
            "surface-2", "#1A2721",
            "sunken", "#0B120F",
            "ink", "#E5EEE8",
            "ink-2", "#C3D2C9",
            "muted", "#93A79D",
            "line", "#26372F",
            "line-strong", "#36493F",
            "jade", "#55BF92",
            "jade-strong", "#78D3AB",
            "jade-soft", "#1C3A2E",
            "on-jade", "#0B1A14",
            "turmeric", "#F0B43C",
            "turmeric-soft", "#3A2F14",
            "beet", "#E2709F",
            "beet-soft", "#3B1E2B",
            "blueberry", "#93A4EA",
            "blueberry-soft", "#20284A",
            "millet", "#CFA669",
            "millet-soft", "#332A1C",
            "chili", "#F2836B",
            "chili-soft", "#3D1F18",
            "veg", "#5CC271",
            "nonveg", "#E0825F",
            "egg", "#F0B43C",
            "focus", "#F0B43C",
            "shadow", "0 1px 2px rgba(0,0,0,.3), 0 10px 30px -12px rgba(0,0,0,.6)",
            "overlay", "rgba(0,0,0,.6)");
}
