package com.oraculum.economy.service.impl;

import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.api.dto.MacroObservationKey;
import com.oraculum.economy.domain.MacroObservationEntity;
import com.oraculum.economy.repository.MacroObservationRepository;
import com.oraculum.economy.service.mapper.MacroObservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EconomyDataServiceImpl implements EconomyDataApi {

    private final MacroObservationRepository macroObservationRepository;
    private final MacroObservationMapper macroObservationMapper;

    private List<MacroIndicator> getIndicatorsFromObservations(List<MacroObservationDto> observations) {
        return observations.stream()
                .map(MacroObservationDto::indicatorCode)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<MacroObservationKey, MacroObservationEntity> getObservationMapByIndicatorAndDate(List<MacroObservationEntity> observations) {
        return observations.stream()
                .collect(Collectors.toMap(
                        MacroObservationEntity::getKey,
                        dto -> dto
                ));
    }

    @Override
    @Transactional
    public void createOrUpdateObservations(List<MacroObservationDto> observations) {
        var observationEntities = macroObservationRepository.findByIndicatorCodeIn(getIndicatorsFromObservations(observations));
        var observationDtoMap = getObservationMapByIndicatorAndDate(observationEntities);

        for (MacroObservationDto dto : observations) {
            var existingEntity = observationDtoMap.get(dto.getKey());
            if (existingEntity == null) {
                macroObservationRepository.save(macroObservationMapper.toEntity(dto));
            } else if (!Objects.equals(existingEntity.getValue(), dto.value())) {
                existingEntity.setValue(dto.value());
                macroObservationRepository.save(existingEntity);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MacroObservationDto> getObservations(MacroIndicator indicator) {
        return macroObservationRepository.findByIndicatorCodeOrderByObservationDateDesc(indicator).stream()
                .map(macroObservationMapper::toDto)
                .collect(Collectors.toList());
    }

}
