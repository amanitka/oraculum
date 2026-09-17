package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanySecApi;
import com.oraculum.company.repository.SecHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanySecServiceImpl implements CompanySecApi {

    private final SecHoldingRepository secHoldingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctHoldingCusips() {
        return secHoldingRepository.findDistinctCusips();
    }
}
