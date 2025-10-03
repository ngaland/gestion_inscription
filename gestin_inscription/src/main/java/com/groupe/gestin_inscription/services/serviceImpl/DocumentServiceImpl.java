package com.groupe.gestin_inscription.services.serviceImpl;

import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.exceptions.FileValidationException;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Document;
import com.groupe.gestin_inscription.model.Enums.ValidationStatus;
import com.groupe.gestin_inscription.repository.ApplicationRepository;
import com.groupe.gestin_inscription.repository.DocumentRepository;
import com.groupe.gestin_inscription.services.serviceInterfaces.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;


@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private DocumentManagerService documentManagerService; // Represents the Spring Boot backend module

    /**
     * Handles the secure upload of a document to the system.
     * Performs initial format and size validation.
     * @param applicationId The ID of the application the document belongs to.
     * @param docDTO The DTO containing document details and file content.
     * @return The saved Document entity.
     */
    @Override
    public Document uploadDocument(Long applicationId, DocumentUploadRequestDTO docDTO) {
        // Step 1: Preliminary validation based on project specs
        // Assuming docDTO.getFileContent() returns a byte array from a MultipartFile
        MultipartFile fileContent = docDTO.getFileContent();
        log.info("file Content : {}", fileContent);
        // Checking if the file is empty
        if (fileContent.isEmpty()) {
            throw new FileValidationException("Cannot upload an empty file.");
        }
        //checking the type
        if (!documentManagerService.verifyFormat(docDTO.getDocumentType(), fileContent)) {
            throw new FileValidationException("Invalid document format. Accepted types are PDF, JPG, and PNG.");
        }
        //checking the size
        if (fileContent.getSize() > 5 * 1024 * 1024) { // 5MB max size
            throw new FileValidationException("Document exceeds the maximum size of 5MB.");
        }

        // Step 2: Save the document securely (e.g., to a cloud storage or local file system)
        String securePath = null;
        try {
            securePath = documentManagerService.saveSecurely(docDTO.getFileContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Step 3: Create and save the Document entity in the database
        Document document = new Document();
        document.setName(docDTO.getDocumentType());
        document.setFileType(documentManagerService.getFileExtension(fileContent.getName()));
        document.setFileSizeMB((double) fileContent.getSize() / (1024 * 1024));
        document.setFilePath(securePath);
        document.setValidationStatus(ValidationStatus.PENDING);

        // Associate with the application
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        document.setApplication(application);

        return documentRepository.save(document);
    }

    /**
     * Performs automatic validation on a document based on its type.
     * This method is called during the pre-validation step.
     * @param document The Document entity to validate.
     * @return true if validation passes, false otherwise.
     */
    @Override
    public boolean performAutomaticValidation(Document document) {
        // Verify format and size (already done in upload, but good to double-check)
        if (!documentManagerService.verifyFormat(document.getFileType(), null) || document.getFileSizeMB() > 5.0) {
            return false;
        }

        // Specific validations as per project document [cite: 55]
        switch (document.getName()) {
            case "CNI recto/verso":
                // Use OCR to verify information on ID cards
                if (!documentManagerService.performOcrCheck(document.getFilePath())) {
                    return false;
                }
                break;
            case "Acte de naissance":
                // Detect watermarks
                if (!documentManagerService.detectWatermark(document.getFilePath(), document.getFileType())) {
                    return false;
                }
                break;
            case "Photo d'identité":
                // Verify photo ratio and face detection
                if (!documentManagerService.verifyPhotoRatio(document.getFilePath())) {
                    return false;
                }
                break;
            default:
                // General checks for other documents
                break;
        }

        return true;
    }

    /**
     * Performs fraud detection by checking for document copies.
     * @param document The Document entity to check.
     * @return true if a copy is detected, false otherwise.
     */
    @Override
    public boolean detectDocumentCopy(Document document) {
        // Call the backend's data protection service to check for similarity [cite: 81]
        boolean isCopy = documentManagerService.checkForSimilarity(document.getFilePath());

        if (isCopy) {
            // Log the alert for suspicious upload [cite: 82]
            // ...
        }

        return isCopy;
    }

    /**
     * Retrieves a list of documents associated with a specific application.
     * @param applicationId The ID of the application.
     * @return A list of Document entities.
     */
    public List<Document> getDocumentsByApplicationId(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId);
    }

    /**
     * Manually validates a document.
     * This method should only be accessible by an administrator with the 'AGENT' role.
     * @param documentId The ID of the document to validate.
     * @param adminId The ID of the administrator performing the validation.
     */
    @Override
    public void manualValidation(Long documentId, Long adminId) {
        // Role-based access control check (assuming it's done at the controller or aspect level)
        Document document = documentRepository.findById(documentId).orElseThrow();
        document.setValidationStatus(ValidationStatus.VALIDATED);
        documentRepository.save(document);
    }
}
