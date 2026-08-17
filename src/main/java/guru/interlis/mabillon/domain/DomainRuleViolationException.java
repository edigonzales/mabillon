package guru.interlis.mabillon.domain;

public final class DomainRuleViolationException extends ConflictException {

    public DomainRuleViolationException(String message) {
        super(message);
    }
}
