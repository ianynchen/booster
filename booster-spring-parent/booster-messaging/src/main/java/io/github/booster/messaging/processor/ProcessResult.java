package io.github.booster.messaging.processor;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ProcessResult<T> {

    @NonNull
    private T data;

    private boolean acknowledged;
}
