package com.groupe.gestin_inscription.config;

import com.groupe.gestin_inscription.services.serviceInterfaces.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;

@Converter
public class SensitiveDataConverter implements AttributeConverter<String, String> {

    @Autowired
    private EncryptionService encryptionService;

    // Use constructor injection for Autowiring in Converters
    public SensitiveDataConverter() {
        // Fallback for non-Spring environments, but for Spring Boot this will be auto-wired later
    }

    // This is called when reading data from the DB to the entity
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }

    // This is called when writing data from the entity to the DB
    @Override
    public String convertToDatabaseColumn(String entityData) {
        return encryptionService.encrypt(entityData);
    }
}