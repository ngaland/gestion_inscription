package com.groupe.gestin_inscription.services.serviceInterfaces;

public interface EncryptionService {
    String encrypt(String data);
    String decrypt(String encryptedData);
}
