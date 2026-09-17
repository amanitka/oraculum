package com.oraculum.company.service.impl;

import com.oraculum.company.repository.SecHoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
