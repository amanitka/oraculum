package com.oraculum.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NumericUtilTest {

    @Test
    void round1AndRound2_roundToExpectedPrecision() {
        assertThat(NumericUtil.round1(16.68)).isEqualTo(16.7f);
        assertThat(NumericUtil.round1(-23.64)).isEqualTo(-23.6f);
        assertThat(NumericUtil.round2(22.659)).isEqualTo(22.66f);
        assertThat(NumericUtil.round2(4.104)).isEqualTo(4.10f);
    }

    @Test
    void medianDouble_handlesOddEvenAndEmptyInputs() {
        assertThat(NumericUtil.medianDouble(null, 0.20)).isEqualTo(0.20);
        assertThat(NumericUtil.medianDouble(List.of(), 0.20)).isEqualTo(0.20);
        assertThat(NumericUtil.medianDouble(List.of(0.50, 0.10, 0.30), 0.20)).isEqualTo(0.30);
        assertThat(NumericUtil.medianDouble(List.of(0.10, 0.20, 0.40, 0.80), 0.20)).isCloseTo(0.30, within(1e-9));
    }

    @Test
    void medianFloat_handlesOddEvenNullElementsAndEmptyInputs() {
        assertThat(NumericUtil.medianFloat(null)).isNull();
        assertThat(NumericUtil.medianFloat(List.of())).isNull();
        assertThat(NumericUtil.medianFloat(Arrays.asList(null, null))).isNull();
        assertThat(NumericUtil.medianFloat(List.of(30.0f, 10.0f, 20.0f))).isEqualTo(20.0f);
        assertThat(NumericUtil.medianFloat(List.of(10.0f, 20.0f, 30.0f, 40.0f))).isEqualTo(25.0f);
    }

    @Test
    void stringToDouble_parsesValidStringsAndReturnsNullForInvalid() {
        assertThat(NumericUtil.stringToDouble(" 42.5 ")).isEqualTo(42.5);
        assertThat(NumericUtil.stringToDouble(".")).isNull();
        assertThat(NumericUtil.stringToDouble("abc")).isNull();
        assertThat(NumericUtil.stringToDouble(null)).isNull();
    }
}
