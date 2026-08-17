package guru.interlis.mabillon.archivierung;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.persistence.cayenne.ArchivablieferungDossier;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.storage.DocumentStorage;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Component;

@Component
public final class Ech0160SipGenerator implements SipGenerator {

    private static final String NS = "http://bar.admin.ch/arelda/v4";
    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private final CayenneUnitOfWork unitOfWork;
    private final DocumentStorage storage;
    private final ArchivePathConfiguration paths;

    public Ech0160SipGenerator(
            CayenneUnitOfWork unitOfWork,
            DocumentStorage storage,
            ArchivePathConfiguration paths) {
        this.unitOfWork = unitOfWork;
        this.storage = storage;
        this.paths = paths;
    }

    @Override
    public GeneratedSip generate(SipGenerationRequest request) {
        Path packageRoot = request.targetDirectory().toAbsolutePath().normalize();
        if (Files.exists(packageRoot)) {
            throw new IllegalArgumentException("SIP-Ziel existiert bereits: " + packageRoot);
        }
        try {
            Files.createDirectories(packageRoot.resolve("header/xsd"));
            Files.createDirectories(packageRoot.resolve("content"));
            copyXsdFixture(packageRoot.resolve("header/xsd"));
            Snapshot snapshot = unitOfWork.read(context -> snapshot(context, request.deliveryNumber()));
            writeContent(packageRoot, snapshot);
            Files.writeString(packageRoot.resolve("header/metadata.xml"), metadata(snapshot, request.profile()),
                    StandardCharsets.UTF_8);
            return new GeneratedSip(packageRoot, packageSize(packageRoot), packageHash(packageRoot));
        } catch (IOException failure) {
            throw new IllegalStateException("SIP konnte nicht erzeugt werden.", failure);
        } catch (RuntimeException failure) {
            deleteQuietly(packageRoot);
            throw failure;
        }
    }

    private Snapshot snapshot(ObjectContext context, ArchivAblieferungNumber number) {
        Archivablieferung delivery = ObjectSelect.query(Archivablieferung.class)
                .where(Archivablieferung.ABLIEFERUNGSNUMMER.eq(number.value()))
                .selectFirst(context);
        if (delivery == null) {
            throw new IllegalArgumentException("Unbekannte Archivablieferung: " + number.value());
        }
        List<DossierSnapshot> dossiers = delivery.getArchivablieferungDossiers().stream()
                .map(ArchivablieferungDossier::getDossier)
                .sorted(Comparator.comparing(Dossier::getDossiernummer))
                .map(this::snapshot)
                .toList();
        if (dossiers.isEmpty()) {
            throw new IllegalArgumentException("Archivablieferung enthält kein Dossier.");
        }
        return new Snapshot(
                delivery.getAblieferungsnummer(),
                delivery.getTitel(),
                delivery.getErstelltam() == null ? LocalDate.now() : delivery.getErstelltam().toLocalDate(),
                dossiers);
    }

    private DossierSnapshot snapshot(Dossier dossier) {
        String positionCode = dossier.getOrdnungssystemposition() == null
                ? "unbekannt" : dossier.getOrdnungssystemposition().getAcode();
        String positionTitle = dossier.getOrdnungssystemposition() == null
                ? "Unbekannte Registraturplanposition" : dossier.getOrdnungssystemposition().getTitel();
        LocalDate from = dossier.getEroeffnetam() == null ? LocalDate.now() : dossier.getEroeffnetam();
        LocalDate to = dossier.getGeschlossenam() == null ? from : dossier.getGeschlossenam();
        List<DocumentSnapshot> documents = dossier.getUnterlages().stream()
                .filter(Unterlage::isAktenrelevant)
                .filter(document -> !"Storniert".equalsIgnoreCase(document.getAstatus()))
                .sorted(Comparator.comparing(Unterlage::getTitel))
                .map(document -> new DocumentSnapshot(
                        document.getTIliTid(), document.getTitel(), document.getDateiname(),
                        document.getMimetype(), document.getHashsha256(), document.getStorageuri(),
                        document.getUnterlagentyp() == null ? null : document.getUnterlagentyp().getAcode()))
                .toList();
        return new DossierSnapshot(dossier.getDossiernummer(), dossier.getTitel(), positionCode, positionTitle,
                from, to, documents);
    }

    private void writeContent(Path packageRoot, Snapshot snapshot) throws IOException {
        for (DossierSnapshot dossier : snapshot.dossiers()) {
            Path dossierDirectory = packageRoot.resolve("content").resolve(id("dossier", dossier.number()));
            Files.createDirectories(dossierDirectory);
            int index = 1;
            for (DocumentSnapshot document : dossier.documents()) {
                if (document.storageUri() == null || !storage.exists(document.storageUri())) {
                    throw new IllegalStateException("Archivdatei fehlt: " + document.title());
                }
                String extension = extension(document.filename());
                String fileName = "p%06d%s".formatted(index++, extension);
                Path target = dossierDirectory.resolve(fileName).normalize();
                if (!target.startsWith(dossierDirectory)) {
                    throw new IllegalStateException("Ungültiger SIP-Dateiname.");
                }
                try (InputStream input = storage.open(document.storageUri())) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                String actualHash = hash(target);
                if (document.hashSha256() == null || !document.hashSha256().equalsIgnoreCase(actualHash)) {
                    throw new IllegalStateException("Datei-Hash stimmt nicht: " + document.title());
                }
            }
        }
    }

    private String metadata(Snapshot snapshot, SipProfile profile) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<paket xmlns=\"").append(NS).append("\" xmlns:xsi=\"").append(XSI)
                .append("\" xsi:type=\"paketSIP\" schemaVersion=\"")
                .append(escape(profile.archiveProfileVersion())).append("\">\n")
                .append("  <paketTyp>SIP</paketTyp>\n")
                .append("  <inhaltsverzeichnis>\n");
        for (DossierSnapshot dossier : snapshot.dossiers()) {
            String dossierId = id("dossier", dossier.number());
            xml.append("    <ordner>\n      <name>").append(escape(dossierId)).append("</name>\n");
            int index = 1;
            for (DocumentSnapshot document : dossier.documents()) {
                String fileName = "p%06d%s".formatted(index++, extension(document.filename()));
                String fileId = id("file", dossier.number() + "-" + document.tid());
                xml.append("      <datei id=\"").append(fileId).append("\">\n")
                        .append("        <name>").append(escape(fileName)).append("</name>\n")
                        .append("        <originalName>").append(escape(originalName(document.filename(), document.title())))
                        .append("</originalName>\n")
                        .append("        <pruefalgorithmus>SHA-256</pruefalgorithmus>\n")
                        .append("        <pruefsumme>").append(escape(document.hashSha256())).append("</pruefsumme>\n")
                        .append("      </datei>\n");
            }
            xml.append("    </ordner>\n");
        }
        xml.append("  </inhaltsverzeichnis>\n")
                .append("  <ablieferung xsi:type=\"ablieferungGeverSIP\">\n")
                .append("    <ablieferungstyp>GEVER</ablieferungstyp>\n")
                .append("    <ablieferndeStelle>Mabillon</ablieferndeStelle>\n")
                .append("    <bemerkung>").append(escape(snapshot.title())).append(" / Profil ")
                .append(escape(profile.id())).append("</bemerkung>\n")
                .append("    <ablieferungsnummer>").append(escape(snapshot.deliveryNumber())).append("</ablieferungsnummer>\n")
                .append("    <provenienz>\n")
                .append("      <aktenbildnerName>Mabillon</aktenbildnerName>\n")
                .append("      <systemName>Mabillon</systemName>\n")
                .append("      <registratur>Registraturplan</registratur>\n")
                .append("    </provenienz>\n")
                .append("    <ordnungssystem>\n      <name>Registraturplan</name>\n");
        Map<String, List<DossierSnapshot>> positions = new LinkedHashMap<>();
        for (DossierSnapshot dossier : snapshot.dossiers()) {
            positions.computeIfAbsent(dossier.positionCode(), ignored -> new java.util.ArrayList<>()).add(dossier);
        }
        for (List<DossierSnapshot> positionDossiers : positions.values()) {
            DossierSnapshot position = positionDossiers.getFirst();
            xml.append("      <ordnungssystemposition id=\"").append(id("position", position.positionCode()))
                    .append("\">\n        <nummer>").append(escape(position.positionCode())).append("</nummer>\n")
                    .append("        <titel>").append(escape(position.positionTitle())).append("</titel>\n");
            for (DossierSnapshot dossier : positionDossiers) {
                xml.append("        <dossier id=\"").append(id("dossier", dossier.number())).append("\">\n")
                        .append("          <titel>").append(escape(dossier.title())).append("</titel>\n")
                        .append("          <erscheinungsform>digital</erscheinungsform>\n")
                        .append("          <entstehungszeitraum>\n")
                        .append("            <von><datum>").append(dossier.from()).append("</datum></von>\n")
                        .append("            <bis><datum>").append(dossier.to()).append("</datum></bis>\n")
                        .append("          </entstehungszeitraum>\n")
                        .append("          <aktenzeichen>").append(escape(dossier.number())).append("</aktenzeichen>\n")
                        .append("          <eroeffnungsdatum><datum>").append(dossier.from()).append("</datum></eroeffnungsdatum>\n")
                        .append("          <abschlussdatum><datum>").append(dossier.to()).append("</datum></abschlussdatum>\n");
                int index = 1;
                for (DocumentSnapshot document : dossier.documents()) {
                    String fileName = "p%06d%s".formatted(index++, extension(document.filename()));
                    xml.append("          <dokument id=\"").append(id("document", dossier.number() + "-" + document.tid()))
                            .append("\">\n            <titel>").append(escape(document.title())).append("</titel>\n")
                            .append("            <erscheinungsform>digital</erscheinungsform>\n")
                            .append("            <dokumenttyp>").append(escape(document.typeCode() == null ? "Unterlage" : document.typeCode()))
                            .append("</dokumenttyp>\n")
                            .append("            <dateiRef>").append(id("file", dossier.number() + "-" + document.tid()))
                            .append("</dateiRef>\n          </dokument>\n");
                    if (fileName.isEmpty()) {
                        throw new IllegalStateException("Unmöglicher SIP-Dateiname.");
                    }
                }
                xml.append("        </dossier>\n");
            }
            xml.append("      </ordnungssystemposition>\n");
        }
        return xml.append("    </ordnungssystem>\n  </ablieferung>\n</paket>\n").toString();
    }

    private void copyXsdFixture(Path target) throws IOException {
        if (!Files.isDirectory(paths.xsdRoot())) {
            throw new IllegalStateException("eCH-0160-XSD-Fixture fehlt: " + paths.xsdRoot());
        }
        try (var files = Files.list(paths.xsdRoot())) {
            List<Path> xsds = files.filter(path -> path.getFileName().toString().endsWith(".xsd")).toList();
            if (xsds.isEmpty()) {
                throw new IllegalStateException("eCH-0160-XSD-Fixture ist leer: " + paths.xsdRoot());
            }
            for (Path xsd : xsds) {
                Files.copy(xsd, target.resolve(xsd.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String id(String prefix, String value) {
        String safe = value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return prefix + "_" + safe;
    }

    private static String originalName(String filename, String fallback) {
        return filename == null || filename.isBlank() ? fallback : filename.replace('\\', '/');
    }

    private static String extension(String filename) {
        if (filename == null) {
            return ".bin";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return ".bin";
        }
        String extension = filename.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        return extension.matches("\\.[A-Za-z0-9]{1,10}") ? extension.toLowerCase() : ".bin";
    }

    private static long packageSize(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException failure) {
                    throw new IllegalStateException(failure);
                }
            }).sum();
        }
    }

    private static String packageHash(Path root) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    digest.update(root.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                    digest.update(Files.readAllBytes(file));
                    digest.update((byte) 0);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String hash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void deleteQuietly(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (var files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // The original generation failure is more useful to the caller.
                }
            });
        } catch (IOException ignored) {
            // The original generation failure is more useful to the caller.
        }
    }

    private record Snapshot(String deliveryNumber, String title, LocalDate created, List<DossierSnapshot> dossiers) {
    }

    private record DossierSnapshot(
            String number,
            String title,
            String positionCode,
            String positionTitle,
            LocalDate from,
            LocalDate to,
            List<DocumentSnapshot> documents) {
    }

    private record DocumentSnapshot(
            UUID tid,
            String title,
            String filename,
            String mimeType,
            String hashSha256,
            String storageUri,
            String typeCode) {
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
