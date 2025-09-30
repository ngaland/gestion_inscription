package com.groupe.gestin_inscription.dto.response;

import com.groupe.gestin_inscription.model.Document;
import com.groupe.gestin_inscription.model.Enums.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class DocumentResponseDTO {
    private Long id;
    private String name;
    private String fileType;
    private String validationStatus;
    private String ocrNotes;

    public DocumentResponseDTO(Document doc) {
        this.id = doc.getId();
        this.name = doc.getName();
        this.fileType = doc.getFileType();
        this.validationStatus = String.valueOf(doc.getValidationStatus());
        this.ocrNotes = doc.getOcrNotes();
    }



    // Getters and Setters
}
