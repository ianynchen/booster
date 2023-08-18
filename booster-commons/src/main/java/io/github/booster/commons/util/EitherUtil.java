package io.github.booster.commons.util;

import arrow.core.Either;
import arrow.core.EitherKt;
import com.google.common.base.Preconditions;

/**
 * {@link Either} related utility methods for Java classes.
 */
public interface EitherUtil {

    /**
     * Converts a throwable to Either.Left
     * @param t throwable to be converted
     * @return {@link Either}
     * @param <T> Type of right value.
     */
    static <T> Either<Throwable, T> convertThrowable(Throwable t) {
        return new Either.Left<Throwable>(t);
    }

    /**
     * Converts a right value to Either.Right
     * @param data value to be converted
     * @return {@link Either}
     * @param <T> Type of right value.
     */
    static <T> Either<Throwable, T> convertData(T data) {
        return new Either.Right<T>(data);
    }

    /**
     * Get the right value of either.
     * @param either {@link Either} whose right value to be returned.
     * @return The right value of either, or returns null by default if right value doesn't exist.
     * @param <E> Type of left value.
     * @param <T> Type of right value.
     */
    static <E, T> T getRight(Either<E, T> either) {
        return getRightWithDefault(either, null);
    }

    /**
     * Get the right value of either with a user specified default value.
     * @param either {@link Either} whose right value to be returned.
     * @param defaultValue default value to be used.
     * @return The right value of either, or returns <b>defaultValue</b> if right value doesn't exist.
     * @param <E> Type of left value.
     * @param <T> Type of right value.
     */
    static <E, T> T getRightWithDefault(Either<E, T> either, T defaultValue) {
        return either == null ? null : EitherKt.getOrElse(either, obj -> defaultValue);
    }

    /**
     * Check if {@link Either} contains left value.
     * @param either {@link Either} to check.
     * @return True if either is null or it is left, false otherwise.
     * @param <T> Type of right value.
     */
    static <T> boolean isLeft(Either<Throwable, T> either) {
        return either == null || either.isLeft();
    }

    /**
     * Swaps the left right values of an {@link Either}
     * @param either {@link Either} to be swapped.
     * @return new {@link Either} with left right values swapped.
     * @param <T> Type of right value.
     */
    static <T> Either<T, Throwable> swap(Either<Throwable, T> either) {
        Preconditions.checkArgument(either != null, "either cannot be null");
        return either.swap();
    }
}
