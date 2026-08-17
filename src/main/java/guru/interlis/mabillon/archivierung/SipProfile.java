package guru.interlis.mabillon.archivierung;

public record SipProfile(
        String id,
        String displayName,
        String echVersion,
        String archiveProfileVersion) {

    public static final SipProfile ECH_0160_1_3_0 = new SipProfile(
            "ech-0160-1.3.0",
            "eCH-0160 generisches GEVER-SIP",
            "1.3.0",
            "5.1");
}
