package org.offitec.osp.domain.exception;

public class ChassisDoesntExistException extends RuntimeException {
    public ChassisDoesntExistException(String message) {
        super(message);
    }
}
