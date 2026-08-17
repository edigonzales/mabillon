package guru.interlis.mabillon.security;

import java.util.Objects;

public record ActorId(String value) {

    public ActorId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Actor id must not be blank");
        }
    }
}
