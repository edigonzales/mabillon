package guru.interlis.mabillon.unterlage;

import java.io.InputStream;
import java.io.IOException;

public record OpenedDocument(String filename, String mimeType, long size, InputStream content)
        implements AutoCloseable {

    @Override
    public void close() throws IOException {
        content.close();
    }
}
