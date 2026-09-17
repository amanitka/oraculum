package com.oraculum.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class CollectionUtil {

    private CollectionUtil() {}

    /**
     * Creates a lazy Stream of sublists (chunks) of the given maximum size.
     * Safe against null or empty lists, or non-positive sizes.
     *
     * @param list original list
     * @param size maximum size of each chunk
     * @param <T>  element type
     * @return stream of chunk sublists
     */
    public static <T> Stream<List<T>> partitionStream(List<T> list, int size) {
        if (list == null || list.isEmpty() || size <= 0) {
            return Stream.empty();
        }
        return IntStream.iterate(0, i -> i < list.size(), i -> i + size)
                .mapToObj(i -> list.subList(i, Math.min(i + size, list.size())));
    }

    /**
     * Splits a list into sublists of the given maximum size.
     *
     * @param list original list
     * @param size maximum size of each chunk
     * @param <T>  element type
     * @return list of chunks
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        return partitionStream(list, size).toList();
    }

    /**
     * Executes the given action for each partition of the list.
     *
     * @param list   original list
     * @param size   maximum size of each chunk
     * @param action consumer to execute per chunk
     * @param <T>    element type
     */
    public static <T> void forEachPartition(List<T> list, int size, Consumer<List<T>> action) {
        partitionStream(list, size).forEach(action);
    }
}
