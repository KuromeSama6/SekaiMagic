package moe.ku6.sekaimagic.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtil {
    public static double Lerp(double a, double b, double t) {
        // clamped to a and b
        if (t < 0) return a;
        if (t > 1) return b;
        return LerpUnclamped(a, b, t);
    }

    public static double LerpUnclamped(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double InverseLerp(double a, double b, double value) {
        if (a == b) return 0;
        return Clamp((value - a) / (b - a), 0, 1);
    }

    public static double Clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double Clamp01(double value) {
        return Clamp(value, 0, 1);
    }
}
