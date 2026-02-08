package nnsql.util;

import java.util.NoSuchElementException;

sealed public interface Option<T> {
    record Some<T>(T value) implements Option<T> {}
    record None<T>() implements Option<T> {}

    static <T> Option<T> some(T value) {
        return new Some<>(value);
    }

    static <T> Option<T> none() {
        return new None<>();
    }

    static <T> Option<T> ofNullable(T value) {
        if (value == null) {
            return none();
        } else {
            return some(value);
        }
    }

    default boolean isSome() {
        return this instanceof Some;
    }

    default boolean isNone() {
        return this instanceof None;
    }

    default T get() {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> throw new NoSuchElementException("Option.None has no value");
        };
    }

    default T orElse(T defaultValue) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> defaultValue;
        };
    }

    default T orElseGet(java.util.function.Supplier<? extends T> supplier) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> supplier.get();
        };
    }

    

    default <U> Option<U> map(java.util.function.Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Some<T>(var value) -> some(mapper.apply(value));
            case None<T> _ -> none();
        };
    }
}
