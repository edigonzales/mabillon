package guru.interlis.mabillon.storage;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {

    StagedDocument stage(DocumentUpload upload) throws IOException;

    StoredDocument commit(StagedDocument staged, StorageTarget target) throws IOException;

    InputStream open(String storageUri) throws IOException;

    boolean exists(String storageUri);

    void discard(StagedDocument staged) throws IOException;
}
