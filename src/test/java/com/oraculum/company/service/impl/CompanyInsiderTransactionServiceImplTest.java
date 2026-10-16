package com.oraculum.company.service.impl;

import com.oraculum.company.api.dto.InsiderTransactionByPersonDto;
import com.oraculum.company.api.dto.InsiderTransactionSummaryDto;
import com.oraculum.company.api.dto.InsiderTransactionTickerDto;
import com.oraculum.company.domain.InsiderTransactionByPersonEntity;
import com.oraculum.company.domain.InsiderTransactionSummaryEntity;
import com.oraculum.company.domain.InsiderTransactionTickerEntity;
import com.oraculum.company.repository.InsiderTransactionByPersonRepository;
import com.oraculum.company.repository.InsiderTransactionSummaryRepository;
import com.oraculum.company.repository.InsiderTransactionTickerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyInsiderTransactionServiceImplTest {

    @Mock
    private InsiderTransactionTickerRepository tickerRepository;

    @Mock
    private InsiderTransactionSummaryRepository summaryRepository;

    @Mock
    private InsiderTransactionByPersonRepository byPersonRepository;

    @InjectMocks
    private CompanyInsiderTransactionServiceImpl service;

    @Test
    void getInsiderTransactionsLastFilingDate_returnsDate() {
        LocalDateTime now = LocalDateTime.now();
        when(tickerRepository.findMaxFilingDate()).thenReturn(Optional.of(now));

        Optional<LocalDateTime> result = service.getInsiderTransactionsLastFilingDate();

        assertThat(result).contains(now);
    }

    @Test
    void getInsiderTransactionSummaryByTicker_found_returnsDto() {
        InsiderTransactionSummaryEntity entity = new InsiderTransactionSummaryEntity();
        entity.setTicker("AAPL");
        entity.setBuysValue3m(100.0);
        when(summaryRepository.findById("AAPL")).thenReturn(Optional.of(entity));

        Optional<InsiderTransactionSummaryDto> result = service.getInsiderTransactionSummaryByTicker("AAPL");

        assertThat(result).isPresent();
        assertThat(result.get().ticker()).isEqualTo("AAPL");
    }

    @Test
    void getInsiderTransactionsByTicker_returnsList() {
        LocalDate after = LocalDate.of(2026, 1, 1);
        InsiderTransactionTickerEntity entity = new InsiderTransactionTickerEntity();
        entity.setTicker("AAPL");
        when(tickerRepository.findByTickerAndTradeDateAfterOrderByFilingDateDesc("AAPL", after))
                .thenReturn(List.of(entity));

        List<InsiderTransactionTickerDto> result = service.getInsiderTransactionsByTicker("AAPL", after);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ticker()).isEqualTo("AAPL");
    }

    @Test
    void getInsiderTransactionsByPerson_returnsAggregatedList() {
        InsiderTransactionByPersonEntity entity = new InsiderTransactionByPersonEntity();
        entity.setId("NVDA_Stevens");
        entity.setTicker("NVDA");
        entity.setInsiderName("Stevens Mark A");
        entity.setTitle("Dir");
        entity.setValueSold1m(300_000_000.0);
        entity.setValueSold12m(1_100_000_000.0);
        entity.setStakeChangePct12m(-16.0);

        when(byPersonRepository.findByTickerOrderByValueSold12mDesc("NVDA"))
                .thenReturn(List.of(entity));

        List<InsiderTransactionByPersonDto> result = service.getInsiderTransactionsByPerson("NVDA");

        assertThat(result).hasSize(1);
        InsiderTransactionByPersonDto dto = result.getFirst();
        assertThat(dto.ticker()).isEqualTo("NVDA");
        assertThat(dto.insiderName()).isEqualTo("Stevens Mark A");
        assertThat(dto.valueSold1m()).isEqualTo(300_000_000.0);
        assertThat(dto.valueSold12m()).isEqualTo(1_100_000_000.0);
        assertThat(dto.stakeChangePct12m()).isEqualTo(-16.0);
    }
}
