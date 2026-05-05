package nnsql.util;

import java.util.stream.Stream;

public final class Streams {

    private Streams() {
    }

    public static <T> Option<T> firstPresent(Stream<Option<T>> options) {
        return options
            .flatMap(Option::stream)
            .map(Option::some)
            .findFirst()
            .orElse(Option.none());
    }
}
