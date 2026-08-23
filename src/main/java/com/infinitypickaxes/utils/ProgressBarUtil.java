package com.infinitypickaxes.utils;

public final class ProgressBarUtil {

    private ProgressBarUtil() {}

    /**
     * Generates a sleek colored visual progress bar for XP or progression.
     */
    public static String getProgressBar(double current, double max, int totalBars, String symbolCompleted, String symbolUncompleted, String colorCompleted, String colorUncompleted) {
        if (max <= 0) max = 1;
        if (current < 0) current = 0;
        if (current > max) current = max;

        double percent = current / max;
        int completedBars = (int) Math.round(totalBars * percent);
        int uncompletedBars = totalBars - completedBars;

        StringBuilder bar = new StringBuilder();
        bar.append(colorCompleted);
        for (int i = 0; i < completedBars; i++) {
            bar.append(symbolCompleted);
        }
        bar.append(colorUncompleted);
        for (int i = 0; i < uncompletedBars; i++) {
            bar.append(symbolUncompleted);
        }

        return bar.toString();
    }
}
