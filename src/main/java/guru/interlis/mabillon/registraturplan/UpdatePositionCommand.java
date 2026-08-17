package guru.interlis.mabillon.registraturplan;

public record UpdatePositionCommand(
        String code,
        String titel,
        String beschreibung,
        String status) {

    public UpdatePositionCommand {
        if (code == null || code.isBlank() || titel == null || titel.isBlank()) {
            throw new IllegalArgumentException("code and titel must not be blank");
        }
    }
}
