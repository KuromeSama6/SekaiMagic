package moe.ku6.sekaimagic.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<TFirst, TSecond> {
    TFirst first;
    TSecond second;
}
