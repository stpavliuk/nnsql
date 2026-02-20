package nnsql.util;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    default T orElseGet(Supplier<? extends T> supplier) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> supplier.get();
        };
    }

    default <U> Option<U> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Some<T>(var value) -> some(mapper.apply(value));
            case None<T> _ -> none();
        };
    }

    default <U> Option<U> flatMap(Function<? super T, Option<U>> mapper) {
        return switch (this) {
            case Some<T>(var value) -> mapper.apply(value);
            case None<T> _ -> none();
        };
    }

    default Option<T> or(Supplier<Option<T>> supplier) {
        return switch (this) {
            case Some<T> _ -> this;
            case None<T> _ -> supplier.get();
        };
    }

    default Stream<T> stream() {
        return switch (this) {
            case Some<T>(var value) -> Stream.of(value);
            case None<T> _ -> Stream.empty();
        };
    }

    default T orElseThrow(Supplier<RuntimeException> e) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> throw e.get();
        };
    }
}
