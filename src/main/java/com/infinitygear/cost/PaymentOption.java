package com.infinitygear.cost;

import java.util.List;

/** Components are AND; separate PaymentOptions are OR alternatives. */
public record PaymentOption(String id, List<CostComponent> components) {
    public PaymentOption {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Payment option id is required.");
        components = components == null ? List.of() : List.copyOf(components);
    }

    public boolean free() {
        return components.isEmpty();
    }
}
