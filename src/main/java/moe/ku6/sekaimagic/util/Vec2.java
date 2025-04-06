package moe.ku6.sekaimagic.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Vec2 {
    private double x;
    private double y;

    public Vec2() {

    }

    public Vec2(List<Double> values) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Range requires at least two values");
        }
        this.x = values.get(0);
        this.y = values.get(1);
    }

    public Vec2 Add(Vec2 vec) {
        return Add(vec.x, vec.y);
    }
    public Vec2 Add(double x, double y) {
        return new Vec2(this.x + x, this.y + y);
    }

    public double Random() {
        return x + Math.random() * (y - x);
    }

    public Vec2 LerpTo(Vec2 vec, double t) {
        return Lerp(this, vec, t);
    }

    public boolean IsInRange(double value) {
        return value >= x && value <= y;
    }

    public double InverseLerp(double value) {
        return MathUtil.InverseLerp(x, y, value);
    }

    public boolean Overlaps(Vec2 other) {
        return x <= other.y && y >= other.x;
    }

    @Override
    public String toString() {
        return "Vec2(%s, %s)".formatted(x, y);
    }

    public static Vec2 Lerp(Vec2 a, Vec2 b, double t) {
        return new Vec2(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t
        );
    }
}
