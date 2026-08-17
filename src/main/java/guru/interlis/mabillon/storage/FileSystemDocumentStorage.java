package guru.interlis.mabillon.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class FileSystemDocumentStorage implements DocumentStorage {

    private static final String URI_PREFIX = "mabillon:objects/";

    private final Path root;
    private final Path stagingRoot;
    private final Path objectsRoot;
    private final long maxFileSizeBytes;

    public FileSystemDocumentStorage(
            @Value("${mabillon.storage.root:build/document-storage}") String root,
            @Value("${mabillon.storage.max-file-size-bytes:52428800}") long maxFileSizeBytes) {
        if (maxFileSizeBytes < 1) {
            throw new IllegalArgumentException("Maximale Dateigrösse muss positiv sein.");
        }
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.stagingRoot = this.root.resolve("staging").normalize();
        this.objectsRoot = this.root.resolve("objects").normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public StagedDocument stage(DocumentUpload upload) throws IOException {
        Files.createDirectories(stagingRoot);
        String token = UUID.randomUUID().toString();
        Path target = stagingPath(token);
        long size = 0;
        MessageDigest digest = sha256();
        try (InputStream input = upload.content(); OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size += read;
                if (size > maxFileSizeBytes) {
                    throw new IOException("Datei überschreitet die maximale Grösse von " + maxFileSizeBytes + " Bytes.");
                }
            }
        } catch (IOException failure) {
            Files.deleteIfExists(target);
            throw failure;
        }
        return new StagedDocument(token, upload.originalFilename(), upload.mimeType(), size,
                HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public StoredDocument commit(StagedDocument staged, StorageTarget target) throws IOException {
        Path source = stagingPath(staged.token());
        if (!Files.isRegularFile(source)) {
            throw new IOException("Staged-Datei fehlt: " + staged.token());
        }
        if (!staged.sha256().equals(hash(source)) || staged.size() != Files.size(source)) {
            throw new IOException("Staged-Datei ist beschädigt: " + staged.token());
        }
        String first = staged.sha256().substring(0, 2);
        String second = staged.sha256().substring(2, 4);
        Path objectDirectory = objectsRoot.resolve(first).resolve(second).normalize();
        if (!objectDirectory.startsWith(objectsRoot)) {
            throw new IOException("Ungültiger Ablagepfad.");
        }
        Files.createDirectories(objectDirectory);
        Path destination = objectDirectory.resolve(staged.token()).normalize();
        if (!destination.startsWith(objectsRoot)) {
            throw new IOException("Ungültiger Ablagepfad.");
        }
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination);
        }
        return new StoredDocument(uriFor(destination), staged.originalFilename(), staged.mimeType(),
                staged.size(), staged.sha256());
    }

    @Override
    public InputStream open(String storageUri) throws IOException {
        Path object = pathForUri(storageUri);
        if (!Files.isRegularFile(object)) {
            throw new IOException("Ablageobjekt fehlt: " + storageUri);
        }
        return Files.newInputStream(object);
    }

    @Override
    public boolean exists(String storageUri) {
        try {
            return Files.isRegularFile(pathForUri(storageUri));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public void discard(StagedDocument staged) throws IOException {
        Files.deleteIfExists(stagingPath(staged.token()));
        if (staged.sha256() != null && staged.sha256().length() >= 4) {
            Path committed = objectsRoot.resolve(staged.sha256().substring(0, 2))
                    .resolve(staged.sha256().substring(2, 4)).resolve(staged.token()).normalize();
            if (committed.startsWith(objectsRoot)) {
                Files.deleteIfExists(committed);
            }
        }
    }

    private Path stagingPath(String token) {
        if (token == null || !token.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Ungültiges Staging-Token.");
        }
        return stagingRoot.resolve(token).normalize();
    }

    private Path pathForUri(String storageUri) {
        if (storageUri == null || !storageUri.startsWith(URI_PREFIX)) {
            throw new IllegalArgumentException("Ungültige Storage-URI.");
        }
        Path object = objectsRoot.resolve(storageUri.substring(URI_PREFIX.length())).normalize();
        if (!object.startsWith(objectsRoot)) {
            throw new IllegalArgumentException("Storage-URI verlässt den Ablageroot.");
        }
        return object;
    }

    private String uriFor(Path destination) {
        return URI_PREFIX + objectsRoot.relativize(destination).toString().replace('\\', '/');
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 fehlt in der Java-Laufzeit.", impossible);
        }
    }

    private static String hash(Path path) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
