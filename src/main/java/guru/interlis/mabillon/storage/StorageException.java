package guru.interlis.mabillon.storage;

import guru.interlis.mabillon.domain.MabillonException;

public final class StorageException extends MabillonException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
