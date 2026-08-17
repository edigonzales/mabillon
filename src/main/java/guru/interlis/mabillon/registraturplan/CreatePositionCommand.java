package guru.interlis.mabillon.registraturplan;

public record CreatePositionCommand(
        String planCode,
        String code,
        String titel,
        String beschreibung,
        String parentCode,
        String federfuehrendeEinheit) {

    public CreatePositionCommand {
        requireText(planCode, "planCode");
        requireText(code, "code");
        requireText(titel, "titel");
        requireText(federfuehrendeEinheit, "federfuehrendeEinheit");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
