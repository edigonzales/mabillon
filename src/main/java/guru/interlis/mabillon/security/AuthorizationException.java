package guru.interlis.mabillon.security;

import guru.interlis.mabillon.domain.MabillonException;

public final class AuthorizationException extends MabillonException {

    public AuthorizationException(String message) {
        super(message);
    }
}
