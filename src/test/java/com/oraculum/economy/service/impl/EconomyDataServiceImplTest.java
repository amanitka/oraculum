package com.oraculum.economy.service.impl;

import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.economy.domain.MacroObservationEntity;
import com.oraculum.economy.domain.MacroSummaryEntity;
import com.oraculum.economy.repository.MacroObservationRepository;
import com.oraculum.economy.repository.MacroSummaryRepository;
import com.oraculum.economy.service.mapper.MacroObservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EconomyDataServiceImplTest {

    @Mock
    private MacroObservationRepository macroObservationRepository;

    @Mock
    private MacroObservationMapper macroObservationMapper;

    @InjectMocks
    private EconomyDataServiceImpl economyDataService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getHistoricalData_shouldReturnMappedDtos() {
        // Arrange
        MacroIndicator indicator = MacroIndicator.FEDFUNDS;

        MacroObservationEntity entity = new MacroObservationEntity();
        entity.setIndicatorCode(indicator);
        entity.setObservationDate(LocalDate.of(2023, 1, 1));
        entity.setValue(4.5);

        MacroObservationDto dto = new MacroObservationDto(indicator, LocalDate.of(2023, 1, 1), 4.5);

        when(macroObservationRepository.findByIndicatorCodeOrderByObservationDateAsc(indicator))
                .thenReturn(List.of(entity));
        when(macroObservationMapper.toDto(entity))
                .thenReturn(dto);

        // Act
        List<MacroObservationDto> result = economyDataService.getHistoricalData(indicator);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(dto);
    }

    @Test
    void getMacroeconomicSummary_shouldReturnMappedDtosWithBps() {
        MacroSummaryEntity entity = mock(MacroSummaryEntity.class);
        when(entity.getIndicator()).thenReturn(MacroIndicator.GS10);
        when(entity.getLatestDate()).thenReturn(LocalDate.of(2026, 9, 1));
        when(entity.getLatestValue()).thenReturn(4.99);
        when(entity.getValue1yAgo()).thenReturn(4.12);
        when(entity.getYoyChangePct()).thenReturn(21.117);
        when(entity.getYoyChangeBps()).thenReturn(87.0);

        MacroSummaryRepository summaryRepository = mock(MacroSummaryRepository.class);
        when(summaryRepository.findAll()).thenReturn(List.of(entity));

        EconomyDataServiceImpl service = new EconomyDataServiceImpl(macroObservationRepository, macroObservationMapper, summaryRepository);
        List<MacroSummaryDto> summaries = service.getMacroeconomicSummary();

        assertThat(summaries).hasSize(1);
        var dto = summaries.getFirst();
        assertThat(dto.indicator()).isEqualTo(MacroIndicator.GS10);
        assertThat(dto.yoyChangeBps()).isEqualTo(87.0);
    }
}
