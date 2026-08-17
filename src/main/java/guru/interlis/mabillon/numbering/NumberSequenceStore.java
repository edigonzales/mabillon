package guru.interlis.mabillon.numbering;

public interface NumberSequenceStore {

    long next(String organisationCode, NumberObjectType type, int year);
}
