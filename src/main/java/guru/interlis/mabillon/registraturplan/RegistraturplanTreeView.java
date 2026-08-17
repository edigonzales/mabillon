package guru.interlis.mabillon.registraturplan;

import java.util.List;

public record RegistraturplanTreeView(
        RegistraturplanView plan,
        List<RegistraturplanPositionView> roots) {

    public RegistraturplanTreeView {
        roots = List.copyOf(roots);
    }
}
