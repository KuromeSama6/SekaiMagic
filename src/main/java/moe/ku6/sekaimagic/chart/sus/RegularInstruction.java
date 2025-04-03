package moe.ku6.sekaimagic.chart.sus;

import java.util.List;

public record RegularInstruction(
        int measure,
        int noteType,
        int lane,
        int group,
        List<SUSDataPair> data,
        String rawData
) {

}
