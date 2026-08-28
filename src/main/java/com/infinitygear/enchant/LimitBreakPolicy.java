package com.infinitygear.enchant;

public final class LimitBreakPolicy {
    private LimitBreakPolicy() {}
    public enum Failure { NONE, DISABLED, UNSUPPORTED, LOCKED, TARGET_LOCKED, WRONG_SPECIFIC_TARGET,
        ENCHANTMENT_MISSING, PREMATURE, ABSOLUTE_MAXIMUM }
    public record Request(boolean enabled, boolean supported, boolean unlocked, boolean targetUnlocked,
                          boolean specificTargetMatches, int currentLevel, int standardMaximum,
                          int absoluteMaximum) {}
    public static Failure validate(Request request) {
        if (!request.enabled()) return Failure.DISABLED;
        if (!request.supported()) return Failure.UNSUPPORTED;
        if (!request.specificTargetMatches()) return Failure.WRONG_SPECIFIC_TARGET;
        if (!request.unlocked()) return Failure.LOCKED;
        if (!request.targetUnlocked()) return Failure.TARGET_LOCKED;
        if (request.currentLevel() <= 0) return Failure.ENCHANTMENT_MISSING;
        if (request.currentLevel() < request.standardMaximum()) return Failure.PREMATURE;
        if (request.currentLevel() >= request.absoluteMaximum()) return Failure.ABSOLUTE_MAXIMUM;
        return Failure.NONE;
    }
}
