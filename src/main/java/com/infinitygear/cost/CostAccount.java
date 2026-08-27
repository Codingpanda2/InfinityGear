package com.infinitygear.cost;

/** Live player/payment boundary. Implementations must mutate on the server thread. */
public interface CostAccount {
    enum Check { AVAILABLE, INSUFFICIENT, PROVIDER_UNAVAILABLE, INVALID_CONFIGURATION }

    Check check(CostComponent component);

    /** Must either withdraw the complete component or return false without mutation. */
    boolean withdraw(CostComponent component);

    /** Compensates a successful withdrawal when a later component or operation fails. */
    void refund(CostComponent component);
}
