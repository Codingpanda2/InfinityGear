package com.infinitygear.cost;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostEngineTest {
    private static CostComponent xp(double amount) {
        return new CostComponent(CostComponent.Type.XP_LEVELS, "", "", amount);
    }
    private static CostComponent money(double amount) {
        return new CostComponent(CostComponent.Type.MONEY, "vault", "", amount);
    }

    @Test void componentsAreAndAndOptionsAreOr() {
        FakeAccount account = new FakeAccount();
        account.states.put(xp(8), CostAccount.Check.AVAILABLE);
        account.states.put(money(15_000), CostAccount.Check.INSUFFICIENT);
        var quotes = new CostEngine().quote(List.of(
                new PaymentOption("xp", List.of(xp(8))),
                new PaymentOption("money-and-xp", List.of(money(15_000), xp(8)))), account);
        assertTrue(quotes.get(0).affordable());
        assertFalse(quotes.get(1).affordable());
    }

    @Test void missingProviderDisablesOnlyItsOptionAndNeverBecomesFree() {
        FakeAccount account = new FakeAccount();
        account.states.put(money(15_000), CostAccount.Check.PROVIDER_UNAVAILABLE);
        var quote = new CostEngine().quote(List.of(new PaymentOption("money", List.of(money(15_000)))), account).getFirst();
        assertFalse(quote.providerUsable());
        assertFalse(quote.affordable());
    }

    @Test void emptyOptionIsExplicitlyFree() {
        assertTrue(new CostEngine().quote(List.of(new PaymentOption("free", List.of())), new FakeAccount())
                .getFirst().affordable());
    }

    @Test void downstreamFailureCompensatesAllComponentsInReverseOrder() {
        FakeAccount account = new FakeAccount();
        CostComponent first = xp(8), second = money(15_000);
        account.states.put(first, CostAccount.Check.AVAILABLE);
        account.states.put(second, CostAccount.Check.AVAILABLE);
        try (var payment = new CostEngine().charge(new PaymentOption("both", List.of(first, second)), account)) {
            // Simulated mutation failure: close without commit.
        }
        assertEquals(List.of(second, first), account.refunded);
    }

    private static final class FakeAccount implements CostAccount {
        final Map<CostComponent, Check> states = new HashMap<>();
        final List<CostComponent> refunded = new ArrayList<>();
        public Check check(CostComponent component) { return states.getOrDefault(component, Check.AVAILABLE); }
        public boolean withdraw(CostComponent component) { return check(component) == Check.AVAILABLE; }
        public void refund(CostComponent component) { refunded.add(component); }
    }
}
