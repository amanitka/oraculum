package com.oraculum.company.service.impl;

import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.company.domain.CompanyEntity;
import com.oraculum.company.repository.CompanyRepository;
import com.oraculum.company.repository.IndustryRepository;
import com.oraculum.company.repository.MarketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyMetadataServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private IndustryRepository industryRepository;

    @Captor
    private ArgumentCaptor<List<CompanyEntity>> companiesCaptor;

    private CompanyMetadataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyMetadataServiceImpl(companyRepository, marketRepository, industryRepository);
    }

    @Test
    void updateCompanyCusips_whenInputEmpty_doesNothing() {
        service.updateCompanyCusips(Map.of());
        verifyNoInteractions(companyRepository);
    }

    @Test
    void updateCompanyCusips_updatesCusipWhenChanged() {
        CompanyEntity company1 = new CompanyEntity();
        company1.setId(1);
        company1.setTicker("AMD");
        company1.setMarket("US");
        company1.setCusip(null);

        CompanyEntity company2 = new CompanyEntity();
        company2.setId(2);
        company2.setTicker("AAPL");
        company2.setMarket("US");
        company2.setCusip("037833100"); // already up to date

        when(companyRepository.findAll()).thenReturn(List.of(company1, company2));

        service.updateCompanyCusips(Map.of(
                new TickerKeyDto("AMD", "US"), "007903107",
                new TickerKeyDto("AAPL", "US"), "037833100"
        ));

        verify(companyRepository).saveAll(companiesCaptor.capture());
        List<CompanyEntity> updated = companiesCaptor.getValue();
        assertThat(updated).hasSize(1);
        assertThat(updated.getFirst().getTicker()).isEqualTo("AMD");
        assertThat(updated.getFirst().getCusip()).isEqualTo("007903107");
    }
}
