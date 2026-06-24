package com.oraculum.company.api.dto;

import com.oraculum.company.domain.InsiderTransactionSummaryEntity;
import lombok.Builder;

@Builder
public record InsiderTransactionSummaryDto(
    String ticker,
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
    Boolean hasClusterBuy
) {
    public static InsiderTransactionSummaryDto fromEntity(InsiderTransactionSummaryEntity entity) {
        return InsiderTransactionSummaryDto.builder()
                .ticker(entity.getTicker())
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
                .build();
    }
}
