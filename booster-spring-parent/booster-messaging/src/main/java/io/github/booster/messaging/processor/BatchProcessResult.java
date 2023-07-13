package io.github.booster.messaging.processor;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class BatchProcessResult<T> {

    private List<T> data;

    private int acknowledged;

    private int unacknowledged;

    private int failed;
}
