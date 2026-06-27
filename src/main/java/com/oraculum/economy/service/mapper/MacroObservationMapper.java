package com.oraculum.economy.service.mapper;

import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.domain.MacroObservationEntity;
import org.springframework.stereotype.Component;

@Component
public class MacroObservationMapper {

    public MacroObservationDto toDto(MacroObservationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MacroObservationDto(
                entity.getIndicatorCode(),
                entity.getObservationDate(),
                entity.getValue()
        );
    }

    public MacroObservationEntity toEntity(MacroObservationDto dto) {
        if (dto == null) {
            return null;
        }
        MacroObservationEntity entity = new MacroObservationEntity();
        entity.setIndicatorCode(dto.indicatorCode());
        entity.setObservationDate(dto.observationDate());
        entity.setValue(dto.value());
        return entity;
    }
}
