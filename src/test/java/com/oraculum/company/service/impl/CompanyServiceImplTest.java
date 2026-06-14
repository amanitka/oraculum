package com.oraculum.company.service.impl;

import com.oraculum.common.exception.EntityNotFoundException;
import com.oraculum.company.api.dto.*;
import com.oraculum.company.domain.*;
import com.oraculum.company.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private MarketRepository marketRepository;
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private NewsTickerRepository newsTickerRepository;
    @Mock
    private BalanceSheetRepository balanceSheetRepository;
    @Mock
    private SharePriceRepository sharePriceRepository;
    @Mock
    private ScreenerMasterRepository screenerMasterRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private CompanyEntity testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new CompanyEntity();
        testCompany.setId(1);
        testCompany.setTicker("AAPL");
        testCompany.setCompanyName("Apple Inc.");
    }

    @Test
    void getCompanyById_whenExists_returnsDto() {
        when(companyRepository.findById(1)).thenReturn(Optional.of(testCompany));

        CompanyDto result = companyService.getCompanyById(1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1);
        assertThat(result.ticker()).isEqualTo("AAPL");
    }

    @Test
    void getCompanyById_whenNotExists_throwsException() {
        when(companyRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(999))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllCompanies_returnsSortedDtos() {
        CompanyEntity company2 = new CompanyEntity();
        company2.setId(2);
        company2.setTicker("MSFT");

        when(companyRepository.findAll()).thenReturn(List.of(company2, testCompany)); // MSFT, AAPL

        List<CompanyDto> results = companyService.getAllCompanies();

        assertThat(results).hasSize(2);
        assertThat(results.getFirst().ticker()).isEqualTo("AAPL"); // AAPL should be first
    }

    @Test
    void getAllMarketIds_returnsIds() {
        when(marketRepository.findAllMarketIds()).thenReturn(List.of("US", "EU"));

        List<String> results = companyService.getAllMarketIds();

        assertThat(results).containsExactly("US", "EU");
    }

    @Test
    void getNewsByTicker_joinsAndSortsCorrectly() {
        LocalDate afterDate = LocalDate.of(2023, 1, 1);
        OffsetDateTime afterDateTime = afterDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

        NewsTickerEntity tickerEntity = new NewsTickerEntity();
        tickerEntity.setNewsId("n1");
        tickerEntity.setTicker("AAPL");
        tickerEntity.setTimePublished(OffsetDateTime.parse("2023-01-02T10:00:00Z"));

        NewsEntity newsEntity = new NewsEntity();
        newsEntity.setId("n1");
        newsEntity.setTitle("Apple News");

        when(newsTickerRepository.findByTickerAndTimePublishedAfter("AAPL", afterDateTime))
                .thenReturn(List.of(tickerEntity));
        when(newsRepository.findByIdIn(List.of("n1")))
                .thenReturn(List.of(newsEntity));

        List<NewsTickerDto> results = companyService.getNewsByTicker("AAPL", afterDate);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo("n1");
        assertThat(results.getFirst().ticker()).isEqualTo("AAPL");
    }

    @Test
    void getBalanceSheetsByCompanyId_sortsCorrectly() {
        LocalDate after = LocalDate.of(2022, 1, 1);

        BalanceSheetEntity bs1 = new BalanceSheetEntity();
        bs1.setFiscalYear(2022);
        bs1.setFiscalPeriod("Q1");

        BalanceSheetEntity bs2 = new BalanceSheetEntity();
        bs2.setFiscalYear(2023);
        bs2.setFiscalPeriod("Q1");

        when(balanceSheetRepository.findByCompanyIdAndReportDateAfter(1, after))
                .thenReturn(List.of(bs1, bs2));

        List<BalanceSheetDto> results = companyService.getBalanceSheetsByCompanyId(1, after);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).fiscalYear()).isEqualTo(2023); // Descending year
        assertThat(results.get(1).fiscalYear()).isEqualTo(2022);
    }

    @Test
    void getSharePricesByCompanyId_sortsCorrectly() {
        LocalDate after = LocalDate.of(2023, 1, 1);

        SharePriceEntity sp1 = new SharePriceEntity();
        sp1.setTradeDate(LocalDate.of(2023, 1, 5));

        SharePriceEntity sp2 = new SharePriceEntity();
        sp2.setTradeDate(LocalDate.of(2023, 1, 10));

        when(sharePriceRepository.findByCompanyIdAndTradeDateAfter(1, after))
                .thenReturn(List.of(sp1, sp2));

        List<SharePriceDto> results = companyService.getSharePricesByCompanyId(1, after);

        assertThat(results).hasSize(2);
        assertThat(results.getFirst().tradeDate()).isEqualTo(LocalDate.of(2023, 1, 10)); // Descending trade date
    }

    @Test
    void getMasterScreener_sortsNullsLast() {
        ScreenerMasterEntity sc1 = new ScreenerMasterEntity();
        sc1.setTicker("AAPL");
        sc1.setQualityRank(1L);

        ScreenerMasterEntity sc2 = new ScreenerMasterEntity();
        sc2.setTicker("UNKNOWN");
        sc2.setQualityRank(null);

        ScreenerMasterEntity sc3 = new ScreenerMasterEntity();
        sc3.setTicker("MSFT");
        sc3.setQualityRank(2L);

        when(screenerMasterRepository.findAll()).thenReturn(List.of(sc2, sc1, sc3));

        List<ScreenerMasterDto> results = companyService.getMasterScreener();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).ticker()).isEqualTo("AAPL");
        assertThat(results.get(1).ticker()).isEqualTo("MSFT");
        assertThat(results.get(2).ticker()).isEqualTo("UNKNOWN"); // Nulls last
    }

    @Test
    void createOrUpdateNewsBatch_savesBothRepositories() {
        NewsArticleDto article = mock(NewsArticleDto.class);
        when(article.toNewsEntity()).thenReturn(new NewsEntity());
        when(article.toNewsTickerEntities()).thenReturn(List.of(new NewsTickerEntity()));

        companyService.createOrUpdateNewsBatch(List.of(article));

        verify(newsRepository, times(1)).saveAll(any());
        verify(newsTickerRepository, times(1)).saveAll(any());
    }
}
