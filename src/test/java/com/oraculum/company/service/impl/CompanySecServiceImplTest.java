package com.oraculum.company.service.impl;

import com.oraculum.company.repository.SecHoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanySecServiceImplTest {

    @Mock
    private SecHoldingRepository secHoldingRepository;

    private CompanySecServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanySecServiceImpl(secHoldingRepository);
    }

    @Test
    void getDistinctHoldingCusips_delegatesToSecHoldingRepository() {
        when(secHoldingRepository.findDistinctCusips()).thenReturn(List.of("007903107", "037833100"));

        List<String> result = service.getDistinctHoldingCusips();

        assertThat(result).containsExactly("007903107", "037833100");
        verify(secHoldingRepository).findDistinctCusips();
    }

    @Test
    void hasHoldingsForPeriod_whenExists_returnsTrue() {
        LocalDate period = LocalDate.of(2026, 9, 30);
        when(secHoldingRepository.existsByReportPeriod(period)).thenReturn(true);

        boolean exists = service.hasHoldingsForPeriod(period);

        assertThat(exists).isTrue();
        verify(secHoldingRepository).existsByReportPeriod(period);
    }

    @Test
    void hasHoldingsForPeriod_whenMissing_returnsFalse() {
        LocalDate period = LocalDate.of(2026, 9, 30);
        when(secHoldingRepository.existsByReportPeriod(period)).thenReturn(false);

        boolean exists = service.hasHoldingsForPeriod(period);

        assertThat(exists).isFalse();
        verify(secHoldingRepository).existsByReportPeriod(period);
    }
}
