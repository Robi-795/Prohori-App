package com.prohori.app.models;

/**
 * Risk levels used everywhere in Prohori. The score thresholds are part of the
 * transparent rule set and are also listed in the README.
 */
public enum RiskLevel {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1),
    SAFE("Safe", 0);

    public static final int CRITICAL_MIN = 85;
    public static final int HIGH_MIN = 60;
    public static final int MEDIUM_MIN = 35;
    public static final int LOW_MIN = 12;

    public final String label;
    public final int severity;

    RiskLevel(String label, int severity) {
        this.label = label;
        this.severity = severity;
    }

    public static RiskLevel fromScore(int score) {
        if (score >= CRITICAL_MIN) return CRITICAL;
        if (score >= HIGH_MIN) return HIGH;
        if (score >= MEDIUM_MIN) return MEDIUM;
        if (score >= LOW_MIN) return LOW;
        return SAFE;
    }

    public static RiskLevel fromLabel(String label) {
        for (RiskLevel level : values()) {
            if (level.label.equalsIgnoreCase(label)) return level;
        }
        return SAFE;
    }

    /** Critical and High findings create security alerts. */
    public boolean isAlertLevel() {
        return severity >= HIGH.severity;
    }

    /** Medium and above count as "apps to review". */
    public boolean needsReview() {
        return severity >= MEDIUM.severity;
    }
}
