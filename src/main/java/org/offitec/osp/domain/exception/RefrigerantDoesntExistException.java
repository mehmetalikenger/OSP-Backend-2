package org.offitec.osp.domain.exception;

public class RefrigerantDoesntExistException extends RuntimeException {
    public RefrigerantDoesntExistException(String message) {
        super(message);
    }
}
