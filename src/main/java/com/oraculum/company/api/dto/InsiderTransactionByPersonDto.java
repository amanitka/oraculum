package com.oraculum.company.api.dto;

import com.oraculum.company.domain.InsiderTransactionByPersonEntity;

public record InsiderTransactionByPersonDto(
        String ticker,
        String insiderName,
        String title,
        Long txCount1m,
        Double sharesBought1m,
        Double sharesSold1m,
        Double valueSold1m,
        Long txCount3m,
        Double sharesBought3m,
        Double sharesSold3m,
        Double valueSold3m,
        Long txCount6m,
        Double sharesBought6m,
        Double sharesSold6m,
        Double valueSold6m,
        Long txCount12m,
        Double sharesBought12m,
        Double sharesSold12m,
        Double valueSold12m,
        Double initialOwned12m,
        Double currentOwned,
        Double stakeChangePct12m
) {
    public static InsiderTransactionByPersonDto fromEntity(InsiderTransactionByPersonEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InsiderTransactionByPersonDto(
                entity.getTicker(),
                entity.getInsiderName(),
                entity.getTitle(),
                entity.getTxCount1m(),
                entity.getSharesBought1m(),
                entity.getSharesSold1m(),
                entity.getValueSold1m(),
                entity.getTxCount3m(),
                entity.getSharesBought3m(),
                entity.getSharesSold3m(),
                entity.getValueSold3m(),
                entity.getTxCount6m(),
                entity.getSharesBought6m(),
                entity.getSharesSold6m(),
                entity.getValueSold6m(),
                entity.getTxCount12m(),
                entity.getSharesBought12m(),
                entity.getSharesSold12m(),
                entity.getValueSold12m(),
                entity.getInitialOwned12m(),
                entity.getCurrentOwned(),
                entity.getStakeChangePct12m()
        );
    }
}
