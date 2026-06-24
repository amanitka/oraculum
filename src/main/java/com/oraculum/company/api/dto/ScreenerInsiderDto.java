package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.CompanySize;
import com.oraculum.company.domain.ScreenerInsiderEntity;
import lombok.Builder;

@Builder
public record ScreenerInsiderDto(
        String ticker,
        String market,
        Long companyId,
        String companyName,
        String sector,
        String industry,
        String currency,
        CompanySize companySize,
        Double marketCap,
        Double buysValue3m,
        Double sellsValue3m,
        Integer csuiteBuysCount3m,
        Double csuiteBuysValue3m,
        Double buysValue6m,
        Double sellsValue6m,
        Integer csuiteBuysCount6m,
        Double csuiteBuysValue6m,
        Double buysValue12m,
        Double sellsValue12m,
        Integer csuiteBuysCount12m,
        Double csuiteBuysValue12m,
        Boolean hasClusterBuy,
        Float newsSentimentScore,
        String newsSentimentLabel,
        Integer newsCount30d
) {
    public static ScreenerInsiderDto fromEntity(ScreenerInsiderEntity entity) {
        return ScreenerInsiderDto.builder()
                .ticker(entity.getTicker())
                .market(entity.getMarket())
                .companyId((long) entity.getCompanyId())
                .companyName(entity.getCompanyName())
                .sector(entity.getSectorName())
                .industry(entity.getIndustryName())
                .currency(entity.getCurrency())
                .companySize(entity.getCompanySize())
                .marketCap(entity.getMarketCapitalization() != null ? (double) entity.getMarketCapitalization() : null)
                .buysValue3m(entity.getBuysValue3m())
                .sellsValue3m(entity.getSellsValue3m())
                .csuiteBuysCount3m(entity.getCsuiteBuysCount3m())
                .csuiteBuysValue3m(entity.getCsuiteBuysValue3m())
                .buysValue6m(entity.getBuysValue6m())
                .sellsValue6m(entity.getSellsValue6m())
                .csuiteBuysCount6m(entity.getCsuiteBuysCount6m())
                .csuiteBuysValue6m(entity.getCsuiteBuysValue6m())
                .buysValue12m(entity.getBuysValue12m())
                .sellsValue12m(entity.getSellsValue12m())
                .csuiteBuysCount12m(entity.getCsuiteBuysCount12m())
                .csuiteBuysValue12m(entity.getCsuiteBuysValue12m())
                .hasClusterBuy(entity.getHasClusterBuy())
                .newsSentimentScore(entity.getNewsSentimentScore())
                .newsSentimentLabel(entity.getNewsSentimentLabel())
                .newsCount30d(entity.getNewsCount30d())
                .build();
    }
}
