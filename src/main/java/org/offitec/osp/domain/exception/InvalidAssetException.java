package org.offitec.osp.domain.exception;

// Thrown when a client-supplied asset (presign request or upload confirmation)
// fails server-side validation: unknown type, disallowed content type, size over
// the limit, or a storage key that is not scoped to the target unit.
public class InvalidAssetException extends RuntimeException {

    public InvalidAssetException(String message) {
        super(message);
    }
}
