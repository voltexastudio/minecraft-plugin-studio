package com.voltexastudios.voltexa.api;

/**
 * A function that may throw checked exceptions. Used by
 * {@link DatabaseService#withConnection(ThrowingFunction)} so callers can write
 * plain JDBC code without try/catch noise.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> {

    /** Applies the function to {@code input}. */
    R apply(T input) throws Exception;
}
