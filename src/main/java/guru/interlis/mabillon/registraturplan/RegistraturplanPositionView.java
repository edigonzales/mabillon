package guru.interlis.mabillon.registraturplan;

import java.util.List;

public record RegistraturplanPositionView(
        String code,
        String titel,
        String beschreibung,
        String status,
        String planCode,
        String parentCode,
        String federfuehrendeEinheit,
        boolean leaf,
        List<RegistraturplanPositionView> children) {

    public RegistraturplanPositionView {
        children = List.copyOf(children);
    }

    public boolean active() {
        return "aktiv".equalsIgnoreCase(status);
    }
}
