package moe.ku6.sekaimagic.chart.sus;

public record SUSDataPair(
        int data,
        int width
) {
    public boolean IsEmpty() {
        return width == 0;
    }
}
