package guru.interlis.mabillon.geschaeft;

import java.util.List;
import java.util.Map;

import guru.interlis.mabillon.aufgabe.AufgabeView;

public record GeschaeftskontrolleView(
        List<GeschaeftView> offeneGeschaefte,
        List<GeschaeftView> ueberfaelligeGeschaefte,
        List<AufgabeView> offeneAufgaben,
        List<AufgabeView> ueberfaelligeAufgaben,
        Map<String, Long> verteilungNachProzessstatus,
        List<GeschaeftView> inaktiveGeschaefte) {

    public GeschaeftskontrolleView {
        offeneGeschaefte = List.copyOf(offeneGeschaefte);
        ueberfaelligeGeschaefte = List.copyOf(ueberfaelligeGeschaefte);
        offeneAufgaben = List.copyOf(offeneAufgaben);
        ueberfaelligeAufgaben = List.copyOf(ueberfaelligeAufgaben);
        verteilungNachProzessstatus = Map.copyOf(verteilungNachProzessstatus);
        inaktiveGeschaefte = List.copyOf(inaktiveGeschaefte);
    }
}
