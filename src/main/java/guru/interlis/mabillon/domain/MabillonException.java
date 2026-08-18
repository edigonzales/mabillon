package guru.interlis.mabillon.domain;

public abstract class MabillonException extends RuntimeException {

    protected MabillonException(String message) {
        super(message);
    }

    protected MabillonException(String message, Throwable cause) {
        super(message, cause);
    }
}
