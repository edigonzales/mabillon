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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import guru.interlis.mabillon.archivierung.ech0160.generated.*;
import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Archivablieferung;
import guru.interlis.mabillon.persistence.cayenne.ArchivablieferungDossier;
import guru.interlis.mabillon.persistence.cayenne.Dossier;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.storage.DocumentStorage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public final class Ech0160SipGenerator implements SipGenerator {

    private static final String GENERATED_PACKAGE = "guru.interlis.mabillon.archivierung.ech0160.generated";
    private static final JAXBContext JAXB_CONTEXT = createJaxbContext();

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
            writeMetadata(packageRoot.resolve("header/metadata.xml"), snapshot, request.profile());
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
        return new Snapshot(delivery.getAblieferungsnummer(), delivery.getTitel(), dossiers);
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
                String fileName = "p%06d%s".formatted(index++, extension(document.filename()));
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

    private void writeMetadata(Path metadataFile, Snapshot snapshot, SipProfile profile) {
        try {
            ObjectFactory factory = new ObjectFactory();
            Map<String, Datei> filesById = new LinkedHashMap<>();

            Inhaltsverzeichnis contents = new Inhaltsverzeichnis();
            for (DossierSnapshot dossier : snapshot.dossiers()) {
                Ordner folder = new Ordner();
                folder.setName(id("dossier", dossier.number()));
                int index = 1;
                for (DocumentSnapshot document : dossier.documents()) {
                    String fileId = fileId(dossier, document);
                    Datei file = new Datei();
                    file.setId(fileId);
                    file.setName("p%06d%s".formatted(index++, extension(document.filename())));
                    file.setOriginalName(originalName(document.filename(), document.title()));
                    file.setPruefalgorithmus(Pruefalgorithmus.fromValue("SHA-256"));
                    file.setPruefsumme(document.hashSha256());
                    folder.getDatei().add(file);
                    filesById.put(fileId, file);
                }
                contents.getOrdner().add(folder);
            }

            AblieferungGeverSIP delivery = new AblieferungGeverSIP();
            delivery.setAblieferungstyp(Ablieferungstyp.fromValue("GEVER"));
            delivery.setAblieferndeStelle("Mabillon");
            delivery.setBemerkung(snapshot.title() + " / Profil " + profile.id());
            delivery.setAblieferungsnummer(snapshot.deliveryNumber());

            ProvenienzGever provenance = new ProvenienzGever();
            provenance.setAktenbildnerName("Mabillon");
            provenance.setSystemName("Mabillon");
            provenance.setRegistratur("Registraturplan");
            delivery.setProvenienz(provenance);

            OrdnungssystemGever filingSystem = new OrdnungssystemGever();
            filingSystem.setName("Registraturplan");
            Map<String, List<DossierSnapshot>> positions = new LinkedHashMap<>();
            for (DossierSnapshot dossier : snapshot.dossiers()) {
                positions.computeIfAbsent(dossier.positionCode(), ignored -> new java.util.ArrayList<>()).add(dossier);
            }
            for (List<DossierSnapshot> positionDossiers : positions.values()) {
                DossierSnapshot position = positionDossiers.getFirst();
                OrdnungssystempositionGever filingPosition = new OrdnungssystempositionGever();
                filingPosition.setId(id("position", position.positionCode()));
                filingPosition.setNummer(position.positionCode());
                filingPosition.setTitel(position.positionTitle());

                for (DossierSnapshot dossier : positionDossiers) {
                    DossierGever archiveDossier = new DossierGever();
                    archiveDossier.setId(id("dossier", dossier.number()));
                    archiveDossier.setTitel(dossier.title());
                    archiveDossier.setErscheinungsform(ErscheinungsformDossier.fromValue("digital"));
                    archiveDossier.setEntstehungszeitraum(period(dossier.from(), dossier.to()));
                    archiveDossier.setAktenzeichen(dossier.number());
                    archiveDossier.setEroeffnungsdatum(point(dossier.from()));
                    archiveDossier.setAbschlussdatum(point(dossier.to()));

                    for (DocumentSnapshot document : dossier.documents()) {
                        DokumentGever archiveDocument = new DokumentGever();
                        archiveDocument.setId(id("document", dossier.number() + "-" + document.tid()));
                        archiveDocument.setTitel(document.title());
                        archiveDocument.setErscheinungsform(ErscheinungsformDokument.fromValue("digital"));
                        archiveDocument.setDokumenttyp(document.typeCode() == null ? "Unterlage" : document.typeCode());

                        Datei file = filesById.get(fileId(dossier, document));
                        if (file == null) {
                            throw new IllegalStateException("SIP-Dateireferenz konnte nicht aufgelöst werden.");
                        }
                        DateiRef reference = new DateiRef();
                        reference.getValue().add(file);
                        archiveDocument.getDateiRef().add(reference);
                        archiveDossier.getDokument().add(archiveDocument);
                    }
                    filingPosition.getDossier().add(archiveDossier);
                }
                filingSystem.getOrdnungssystemposition().add(filingPosition);
            }
            delivery.setOrdnungssystem(filingSystem);

            PaketSIP packet = new PaketSIP();
            packet.setPaketTyp(PaketTyp.fromValue("SIP"));
            packet.setSchemaVersion(profile.archiveProfileVersion());
            packet.setInhaltsverzeichnis(contents);
            packet.setAblieferung(delivery);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            var schema = schemaFactory.newSchema(paths.xsdRoot().resolve("arelda.xsd").toFile());

            Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setSchema(schema);
            marshaller.marshal(factory.createPaket(packet), metadataFile.toFile());
        } catch (JAXBException | SAXException failure) {
            throw new IllegalStateException("eCH-0160-Metadaten konnten nicht erzeugt werden.", failure);
        }
    }

    private static HistorischerZeitraum period(LocalDate from, LocalDate to) {
        HistorischerZeitraum period = new HistorischerZeitraum();
        period.setVon(point(from));
        period.setBis(point(to));
        return period;
    }

    private static HistorischerZeitpunkt point(LocalDate date) {
        HistorischerZeitpunkt point = new HistorischerZeitpunkt();
        point.setDatum(date.toString());
        return point;
    }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(GENERATED_PACKAGE);
        } catch (JAXBException failure) {
            throw new ExceptionInInitializerError(failure);
        }
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

    private static String fileId(DossierSnapshot dossier, DocumentSnapshot document) {
        return id("file", dossier.number() + "-" + document.tid());
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

    private record Snapshot(String deliveryNumber, String title, List<DossierSnapshot> dossiers) {
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
}
