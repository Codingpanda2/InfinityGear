package com.infinitypickaxes.gui;

import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantSocketsGuiInteractionPolicyTest {

    private static final Set<InventoryAction> SAFE_BOTTOM_ACTIONS = EnumSet.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME,
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR
    );

    @ParameterizedTest
    @EnumSource(InventoryAction.class)
    void onlyOrdinaryBottomInventoryCursorActionsAreAllowed(InventoryAction action) {
        assertEquals(SAFE_BOTTOM_ACTIONS.contains(action),
                EnchantSocketsGui.isSafeBottomAction(action), action.name());
    }

    @Test
    void upgradeLoreRequiresTheNextStrictlyHigherBookLevel() {
        assertEquals(1, EnchantSocketsGui.requiredBookLevel(0));
        assertEquals(4, EnchantSocketsGui.requiredBookLevel(3));
    }
}
