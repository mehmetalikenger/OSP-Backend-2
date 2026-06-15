package org.offitec.osp.domain.service;

import org.offitec.osp.domain.exception.ModelAlreadyExistsException;
import org.offitec.osp.domain.exception.ModelAlreadyExistsException;
import org.offitec.osp.domain.port.ChassisRepositoryPort;
import org.offitec.osp.domain.port.CompressorRepositoryPort;
import org.offitec.osp.domain.port.CondenserRepositoryPort;
import org.offitec.osp.domain.port.EvaporatorRepositoryPort;
import org.offitec.osp.domain.port.ExpansionValveRepositoryPort;
import org.offitec.osp.domain.port.FourWayReversingValveRepositoryPort;
import org.offitec.osp.domain.port.RefrigerantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ComponentDomainService {

    private final CompressorRepositoryPort compressorRepositoryPort;
    private final EvaporatorRepositoryPort evaporatorRepositoryPort;
    private final CondenserRepositoryPort condenserRepositoryPort;
    private final ExpansionValveRepositoryPort expansionValveRepositoryPort;
    private final FourWayReversingValveRepositoryPort fourWayReversingValveRepositoryPort;
    private final ChassisRepositoryPort chassisRepositoryPort;
    private final RefrigerantRepositoryPort refrigerantRepositoryPort;

    public ComponentDomainService(CompressorRepositoryPort compressorRepositoryPort, EvaporatorRepositoryPort evaporatorRepositoryPort, CondenserRepositoryPort condenserRepositoryPort, ExpansionValveRepositoryPort expansionValveRepositoryPort, FourWayReversingValveRepositoryPort fourWayReversingValveRepositoryPort, ChassisRepositoryPort chassisRepositoryPort, RefrigerantRepositoryPort refrigerantRepositoryPort) {
        this.compressorRepositoryPort = compressorRepositoryPort;
        this.evaporatorRepositoryPort = evaporatorRepositoryPort;
        this.condenserRepositoryPort = condenserRepositoryPort;
        this.expansionValveRepositoryPort = expansionValveRepositoryPort;
        this.fourWayReversingValveRepositoryPort = fourWayReversingValveRepositoryPort;
        this.chassisRepositoryPort = chassisRepositoryPort;
        this.refrigerantRepositoryPort = refrigerantRepositoryPort;
    }

    public void validateUniqueModel(String model){

        if(compressorRepositoryPort.existsByModel(model)){

            throw new ModelAlreadyExistsException("Compressor model already exists.");
        }
    }

    public void validateUniqueModelForEdit(String model, Long id){

        if(compressorRepositoryPort.existsByModelAndIdNot(model, id)){

            throw new ModelAlreadyExistsException("Compressor model already exists for another compressor.");
        }
    }

    public void validateUniqueModelForEvaporator(String model){

        if(evaporatorRepositoryPort.existsByModel(model)){

            throw new ModelAlreadyExistsException("Evaporator model already exists.");
        }
    }

    public void validateUniqueModelForEditEvaporator(String model, Long id){

        if(evaporatorRepositoryPort.existsByModelAndIdNot(model, id)){

            throw new ModelAlreadyExistsException("Evaporator model already exists for another evaporator.");
        }
    }

    public void validateUniqueModelForCondenser(String model){
        if(condenserRepositoryPort.existsByModel(model)){
            throw new ModelAlreadyExistsException("Condenser model already exists.");
        }
    }

    public void validateUniqueModelForEditCondenser(String model, Long id){
        if(condenserRepositoryPort.existsByModelAndIdNot(model, id)){
            throw new ModelAlreadyExistsException("Condenser model already exists for another condenser.");
        }
    }

    public void validateUniqueModelForExpansionValve(String model){
        if(expansionValveRepositoryPort.existsByModel(model)){
            throw new ModelAlreadyExistsException("Expansion Valve model already exists.");
        }
    }

    public void validateUniqueModelForEditExpansionValve(String model, Long id){
        if(expansionValveRepositoryPort.existsByModelAndIdNot(model, id)){
            throw new ModelAlreadyExistsException("Expansion Valve model already exists for another expansion valve.");
        }
    }

    public void validateUniqueModelForFourWayReversingValve(String model){
        if(fourWayReversingValveRepositoryPort.existsByModel(model)){
            throw new ModelAlreadyExistsException("4-Way Reversing Valve model already exists.");
        }
    }

    public void validateUniqueModelForEditFourWayReversingValve(String model, Long id){
        if(fourWayReversingValveRepositoryPort.existsByModelAndIdNot(model, id)){
            throw new ModelAlreadyExistsException("4-Way Reversing Valve model already exists for another valve.");
        }
    }

    public void validateUniqueModelForChassis(String model){
        if(chassisRepositoryPort.existsByModel(model)){
            throw new ModelAlreadyExistsException("Chassis model already exists.");
        }
    }

    public void validateUniqueModelForEditChassis(String model, Long id){
        if(chassisRepositoryPort.existsByModelAndIdNot(model, id)){
            throw new ModelAlreadyExistsException("Chassis model already exists for another chassis.");
        }
    }

    public void validateUniqueCodeForRefrigerant(String code){
        if(refrigerantRepositoryPort.existsByCode(code)){
            throw new ModelAlreadyExistsException("Refrigerant code already exists.");
        }
    }

    public void validateUniqueCodeForEditRefrigerant(String code, Long id){
        if(refrigerantRepositoryPort.existsByCodeAndIdNot(code, id)){
            throw new ModelAlreadyExistsException("Refrigerant code already exists for another refrigerant.");
        }
    }
}
