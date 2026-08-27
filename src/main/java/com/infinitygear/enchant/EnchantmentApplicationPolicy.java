package com.infinitygear.enchant;

/** Pure policy for installing the level represented by a normal enchanted book. */
public final class EnchantmentApplicationPolicy {

    private EnchantmentApplicationPolicy() {}

    public enum Failure {
        NONE,
        NOT_MANAGED_BOOK,
        MULTIPLE_MANAGED_ENCHANTMENTS,
        WRONG_ENCHANTMENT,
        DISABLED,
        LOCKED,
        EQUAL_OR_LOWER_LEVEL,
        ABOVE_STANDARD_MAXIMUM,
        SOCKETS_FULL,
        CONFLICT,
        INCOMPATIBLE_TARGET
    }

    public record Request(
            int managedEnchantmentsOnBook,
            boolean selectedEnchantmentOnBook,
            boolean enabled,
            boolean unlocked,
            int currentLevel,
            int bookLevel,
            int usedSockets,
            int socketLimit,
            int socketCost,
            int standardMaximum,
            int absoluteMaximum,
            boolean conflict,
            boolean compatibleTarget
    ) {}

    public record Decision(
            Failure failure,
            int oldLevel,
            int resultingLevel,
            int currentSocketUsage,
            int resultingSocketUsage,
            int standardMaximum,
            int absoluteMaximum
    ) {
        public boolean allowed() {
            return failure == Failure.NONE;
        }
    }

    public static Decision evaluate(Request request) {
        int oldLevel = Math.max(0, request.currentLevel());
        int bookLevel = Math.max(0, request.bookLevel());
        int used = Math.max(0, request.usedSockets());
        long resultingSocketCount = (long) used
                + (oldLevel == 0 ? Math.max(0, request.socketCost()) : 0);
        int resultingSockets = (int) Math.min(Integer.MAX_VALUE, resultingSocketCount);
        int standardMaximum = Math.max(1, request.standardMaximum());
        int absoluteMaximum = Math.max(standardMaximum, request.absoluteMaximum());

        Failure failure = Failure.NONE;
        if (request.managedEnchantmentsOnBook() < 1) failure = Failure.NOT_MANAGED_BOOK;
        else if (request.managedEnchantmentsOnBook() != 1) failure = Failure.MULTIPLE_MANAGED_ENCHANTMENTS;
        else if (!request.selectedEnchantmentOnBook()) failure = Failure.WRONG_ENCHANTMENT;
        else if (!request.enabled()) failure = Failure.DISABLED;
        else if (!request.unlocked()) failure = Failure.LOCKED;
        else if (bookLevel <= oldLevel) failure = Failure.EQUAL_OR_LOWER_LEVEL;
        else if (bookLevel > standardMaximum) failure = Failure.ABOVE_STANDARD_MAXIMUM;
        // Existing oversocketed equipment is grandfathered: only a newly introduced enchantment needs capacity.
        else if (oldLevel == 0 && resultingSocketCount > Math.max(0, request.socketLimit())) failure = Failure.SOCKETS_FULL;
        else if (oldLevel == 0 && request.conflict()) failure = Failure.CONFLICT;
        else if (oldLevel == 0 && !request.compatibleTarget()) failure = Failure.INCOMPATIBLE_TARGET;

        return new Decision(failure, oldLevel, failure == Failure.NONE ? bookLevel : oldLevel,
                used, failure == Failure.NONE ? resultingSockets : used, standardMaximum, absoluteMaximum);
    }
}
