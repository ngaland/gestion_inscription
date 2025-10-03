package com.groupe.gestin_inscription.services.serviceImpl;

import com.groupe.gestin_inscription.services.serviceInterfaces.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    // IMPORTANT: Inject this key from application.properties
    @Value("${app.encryption.secret}")
    private String secret;

    private SecretKeySpec secretKey;
    private Cipher cipher;

    @PostConstruct
    public void init() throws Exception {
        // Initialize key and cipher using AES (requires proper dependency setup)
        byte[] keyBytes = Arrays.copyOf(secret.getBytes("UTF-8"), 16);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.cipher = Cipher.getInstance("AES");
    }

    @Override
    public String encrypt(String data) {
        if (data == null || data.isEmpty()) return data;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) return encryptedData;
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
