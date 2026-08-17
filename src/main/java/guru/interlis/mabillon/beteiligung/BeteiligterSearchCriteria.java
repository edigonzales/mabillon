package guru.interlis.mabillon.beteiligung;

public record BeteiligterSearchCriteria(String name, String typ, String externeReferenz) {

    public static BeteiligterSearchCriteria empty() {
        return new BeteiligterSearchCriteria(null, null, null);
    }
}
