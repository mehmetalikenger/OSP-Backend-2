package org.offitec.osp.domain.exception;

public class CompressorDoesntExistException extends RuntimeException {
    public CompressorDoesntExistException(String message) {
        super(message);
    }
}
