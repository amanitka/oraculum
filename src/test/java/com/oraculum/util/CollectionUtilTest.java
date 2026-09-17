package com.oraculum.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionUtilTest {

    @Test
    void partition_whenNullOrEmptyList_returnsEmptyList() {
        assertThat(CollectionUtil.partition(null, 5)).isEmpty();
        assertThat(CollectionUtil.partition(List.of(), 5)).isEmpty();
    }

    @Test
    void partition_whenInvalidSize_returnsEmptyList() {
        assertThat(CollectionUtil.partition(List.of("a", "b"), 0)).isEmpty();
        assertThat(CollectionUtil.partition(List.of("a", "b"), -1)).isEmpty();
    }

    @Test
    void partition_whenListSmallerThanSize_returnsSinglePartition() {
        List<String> input = List.of("a", "b", "c");
        List<List<String>> partitions = CollectionUtil.partition(input, 5);

        assertThat(partitions).hasSize(1);
        assertThat(partitions.getFirst()).containsExactly("a", "b", "c");
    }

    @Test
    void partition_whenListDivisibleBySize_returnsEqualPartitions() {
        List<Integer> input = List.of(1, 2, 3, 4);
        List<List<Integer>> partitions = CollectionUtil.partition(input, 2);

        assertThat(partitions).hasSize(2);
        assertThat(partitions.get(0)).containsExactly(1, 2);
        assertThat(partitions.get(1)).containsExactly(3, 4);
    }

    @Test
    void partition_whenListNotDivisibleBySize_lastPartitionContainsRemainder() {
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        List<List<Integer>> partitions = CollectionUtil.partition(input, 2);

        assertThat(partitions).hasSize(3);
        assertThat(partitions.get(0)).containsExactly(1, 2);
        assertThat(partitions.get(1)).containsExactly(3, 4);
        assertThat(partitions.get(2)).containsExactly(5);
    }

    @Test
    void forEachPartition_executesActionForEachPartition() {
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        List<List<Integer>> collected = new ArrayList<>();

        CollectionUtil.forEachPartition(input, 2, collected::add);

        assertThat(collected).hasSize(3);
        assertThat(collected.get(0)).containsExactly(1, 2);
        assertThat(collected.get(1)).containsExactly(3, 4);
        assertThat(collected.get(2)).containsExactly(5);
    }
}
