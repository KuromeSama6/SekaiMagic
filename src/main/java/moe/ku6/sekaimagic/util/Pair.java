package moe.ku6.sekaimagic.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class Pair<TFirst, TSecond> {
    TFirst first;
    TSecond second;
}
