package io.github.booster.commons.util;

import arrow.core.Either;
import arrow.core.EitherKt;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EitherUtilTest {

    @Test
    void shouldReturnDefaultRight() {
        Either<Throwable, Integer> either = new Either.Left<>(new IllegalArgumentException(""));
        assertThat(EitherUtil.getRightWithDefault(either, 2), equalTo(2));
    }

    @Test
    void shouldSwap() {
        Either<Throwable, Integer> either = new Either.Left<>(new IllegalArgumentException(""));
        assertThat(
                EitherUtil.getRightWithDefault(
                        EitherUtil.swap(either), new IllegalStateException("")
                ),
                instanceOf(IllegalArgumentException.class)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> EitherUtil.swap(null)
        );
    }

    @Test
    void shouldVerifyLeft() {
        assertThat(EitherUtil.isLeft(null), equalTo(true));
        assertThat(EitherUtil.isLeft(EitherUtil.convertThrowable(new IllegalArgumentException(""))), equalTo(true));
        assertThat(EitherUtil.isLeft(EitherUtil.convertData(null)), equalTo(false));
        assertThat(EitherUtil.isLeft(EitherUtil.convertData(1)), equalTo(false));
    }

    @Test
    void shouldGetValue() {
        assertThat(EitherUtil.getRight(null), nullValue());
        assertThat(EitherUtil.getRight(EitherUtil.convertData(1)), equalTo(1));
        assertThat(EitherUtil.getRight(EitherUtil.convertThrowable(new IllegalArgumentException(""))), equalTo(null));
    }

    @Test
    void shouldCreateEitherData() {
        Either<Throwable, String> data = EitherUtil.convertData("abc");
        assertThat(data.isRight(), equalTo(true));
        assertThat(EitherKt.getOrElse(data, o -> ""), equalTo("abc"));
    }

    @Test
    void shouldCreateEitherThrowable() {
        Either<Throwable, String> data = EitherUtil.convertThrowable(new IllegalStateException("error"));
        assertThat(data.isLeft(), equalTo(true));
        EitherKt.getOrElse(data, t -> {
            assertThat(t, instanceOf(IllegalStateException.class));
            return "abc";
        });
    }
}
