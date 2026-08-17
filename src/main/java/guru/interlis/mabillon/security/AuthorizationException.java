package guru.interlis.mabillon.security;

public final class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}
