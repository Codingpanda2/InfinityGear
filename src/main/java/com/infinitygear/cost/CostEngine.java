package com.infinitygear.cost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared availability, AND/OR selection, charging, and reverse-order compensation. */
public final class CostEngine {
    public record OptionQuote(PaymentOption option, boolean providerUsable, boolean affordable,
                              List<CostComponent> unavailable, List<CostComponent> insufficient) {}

    public List<OptionQuote> quote(List<PaymentOption> options, CostAccount account) {
        if (account == null) throw new IllegalArgumentException("Cost account is required.");
        if (options == null || options.isEmpty()) return List.of();
        List<OptionQuote> quotes = new ArrayList<>();
        for (PaymentOption option : options) {
            List<CostComponent> unavailable = new ArrayList<>();
            List<CostComponent> insufficient = new ArrayList<>();
            for (CostComponent component : option.components()) {
                switch (account.check(component)) {
                    case AVAILABLE -> { }
                    case INSUFFICIENT -> insufficient.add(component);
                    case PROVIDER_UNAVAILABLE, INVALID_CONFIGURATION -> unavailable.add(component);
                }
            }
            quotes.add(new OptionQuote(option, unavailable.isEmpty(),
                    unavailable.isEmpty() && insufficient.isEmpty(),
                    List.copyOf(unavailable), List.copyOf(insufficient)));
        }
        return List.copyOf(quotes);
    }

    public Payment charge(PaymentOption selected, CostAccount account) {
        OptionQuote quote = quote(List.of(selected), account).getFirst();
        if (!quote.providerUsable()) throw new PaymentException("Payment provider is unavailable.");
        if (!quote.affordable()) throw new PaymentException("Payment option is not affordable.");

        List<CostComponent> withdrawn = new ArrayList<>();
        try {
            for (CostComponent component : selected.components()) {
                if (!account.withdraw(component)) throw new PaymentException("Payment changed during confirmation.");
                withdrawn.add(component);
            }
            return new Payment(account, withdrawn);
        } catch (RuntimeException failure) {
            compensate(account, withdrawn);
            throw failure;
        }
    }

    private static void compensate(CostAccount account, List<CostComponent> withdrawn) {
        List<CostComponent> reverse = new ArrayList<>(withdrawn);
        Collections.reverse(reverse);
        RuntimeException compensationFailure = null;
        for (CostComponent component : reverse) {
            try {
                account.refund(component);
            } catch (RuntimeException failure) {
                if (compensationFailure == null) compensationFailure = failure;
                else compensationFailure.addSuppressed(failure);
            }
        }
        if (compensationFailure != null) throw compensationFailure;
    }

    public static final class Payment implements AutoCloseable {
        private final CostAccount account;
        private final List<CostComponent> withdrawn;
        private boolean committed;

        private Payment(CostAccount account, List<CostComponent> withdrawn) {
            this.account = account;
            this.withdrawn = List.copyOf(withdrawn);
        }

        public void commit() { committed = true; }

        @Override public void close() {
            if (!committed) compensate(account, withdrawn);
        }
    }

    public static final class PaymentException extends IllegalStateException {
        public PaymentException(String message) { super(message); }
    }
}
