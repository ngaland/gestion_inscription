package com.groupe.gestin_inscription.services.serviceInterfaces;

import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.model.Document;

public interface DocumentService {
    public Document uploadDocument(Long applicationId, DocumentUploadRequestDTO docDTO);
    public boolean performAutomaticValidation(Document document);
    public boolean detectDocumentCopy(Document document);
    public void manualValidation(Long documentId, Long adminId);
}
