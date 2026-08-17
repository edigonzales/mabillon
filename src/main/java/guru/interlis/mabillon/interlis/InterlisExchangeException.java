package guru.interlis.mabillon.interlis;

public final class InterlisExchangeException extends RuntimeException {

    public InterlisExchangeException(String message) {
        super(message);
    }

    public InterlisExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
