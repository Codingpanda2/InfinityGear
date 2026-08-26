package com.infinitypickaxes.utils;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextUtilTest {

    @Test
    void componentPlaceholderKeepsEcoGradientOutOfGuiSuffix() {
        Component ecoName = TextUtil.parse("<gradient:#0575E6:#1E3FBA>Dynamite");

        Component actual = TextUtil.parseWithComponent(
                "%enchant_display_name% <gray>[<yellow>Lv. 1<gray>]",
                "%enchant_display_name%", ecoName);

        assertEquals(2, actual.children().size());
        assertEquals(ecoName, actual.children().getFirst());
        assertEquals(TextUtil.parse(" <gray>[<yellow>Lv. 1<gray>]"),
                actual.children().getLast());
    }
}
