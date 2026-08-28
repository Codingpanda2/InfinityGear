package com.infinitygear.gear;

public final class SocketExpansionPolicy {
    private SocketExpansionPolicy() {}
    public enum Failure { NONE, CATALYST_MISSING, AT_MAXIMUM, GRANDFATHERED_OVER_MAXIMUM }
    public record Decision(Failure failure, int current, int resulting, int maximum) {
        public boolean allowed() { return failure == Failure.NONE; }
    }
    public static Decision evaluate(int current, int maximum, boolean catalystPresent) {
        current = Math.max(0, current);
        maximum = Math.max(0, maximum);
        if (!catalystPresent) return new Decision(Failure.CATALYST_MISSING, current, current, maximum);
        if (current > maximum) return new Decision(Failure.GRANDFATHERED_OVER_MAXIMUM, current, current, maximum);
        if (current == maximum) return new Decision(Failure.AT_MAXIMUM, current, current, maximum);
        return new Decision(Failure.NONE, current, current + 1, maximum);
    }
}
