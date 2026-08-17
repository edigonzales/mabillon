package guru.interlis.mabillon.unterlage;

import java.io.IOException;
import java.util.UUID;

import guru.interlis.mabillon.persistence.CayenneUnitOfWork;
import guru.interlis.mabillon.persistence.cayenne.Unterlage;
import guru.interlis.mabillon.storage.DocumentStorage;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

@Service
public final class UnterlageContentService {

    private final CayenneUnitOfWork unitOfWork;
    private final DocumentStorage storage;

    public UnterlageContentService(CayenneUnitOfWork unitOfWork, DocumentStorage storage) {
        this.unitOfWork = unitOfWork;
        this.storage = storage;
    }

    public OpenedDocument open(UUID tid) {
        UnterlageView value = unitOfWork.read(context -> {
            Unterlage document = ObjectSelect.query(Unterlage.class)
                    .where(Unterlage.T_ILI_TID.eq(tid)).selectFirst(context);
            if (document == null) {
                throw new IllegalArgumentException("Unbekannte Unterlage: " + tid);
            }
            return UnterlageQueryService.toView(document);
        });
        if (value.storageUri() == null || !storage.exists(value.storageUri())) {
            throw new IllegalStateException("Ablageobjekt der Unterlage fehlt: " + tid);
        }
        try {
            return new OpenedDocument(value.filename(), value.mimeType(), value.size() == null ? 0 : value.size(),
                    storage.open(value.storageUri()));
        } catch (IOException failure) {
            throw new IllegalStateException("Ablageobjekt konnte nicht geöffnet werden: " + tid, failure);
        }
    }
}
