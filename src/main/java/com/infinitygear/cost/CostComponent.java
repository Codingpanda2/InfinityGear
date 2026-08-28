package com.infinitygear.cost;

import java.util.Locale;
import java.util.Objects;

/** One AND-required component inside a payment option. */
public record CostComponent(Type type, String provider, String itemId, double amount, boolean consumed) {
    public enum Type { MONEY, XP_POINTS, XP_LEVELS, VANILLA_ITEM, EXTERNAL_ITEM, TRACKED_ARTIFACT }

    public CostComponent {
        Objects.requireNonNull(type, "type");
        provider = normalize(provider);
        itemId = normalize(itemId);
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Cost amount must be positive.");
        if ((type == Type.VANILLA_ITEM || type == Type.EXTERNAL_ITEM || type == Type.TRACKED_ARTIFACT)
                && itemId.isEmpty()) throw new IllegalArgumentException("Item costs require an item id.");
        if (type == Type.EXTERNAL_ITEM && provider.isEmpty()) {
            throw new IllegalArgumentException("External item costs require a provider.");
        }
    }

    public CostComponent(Type type, String provider, String itemId, double amount) {
        this(type, provider, itemId, amount, true);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public long wholeAmount() {
        if (Math.rint(amount) != amount) throw new IllegalStateException(type + " requires a whole-number amount.");
        return (long) amount;
    }
}
