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

    @Override
    public String toString() {
        return "Vec2(%s, %s)".formatted(x, y);
    }
}
