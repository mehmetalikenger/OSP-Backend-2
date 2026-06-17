package org.offitec.osp.domain.service;

import org.offitec.osp.domain.exception.ModelAlreadyExistsException;
import org.offitec.osp.domain.port.UnitRepository;
import org.springframework.stereotype.Service;

@Service
public class UnitDomainService {

    private final UnitRepository unitRepository;

    public UnitDomainService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public void validateUniqueModel(String model) {

        if (unitRepository.findByModel(model).isPresent()) {

            throw new ModelAlreadyExistsException("A unit with this model already exists.");
        }
    }

    public void validateUniqueModelForEdit(String model, Long id) {

        unitRepository.findByModel(model).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ModelAlreadyExistsException("A unit with this model already exists.");
            }
        });
    }
}
