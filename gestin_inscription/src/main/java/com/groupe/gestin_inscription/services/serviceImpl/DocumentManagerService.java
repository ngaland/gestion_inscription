package com.groupe.gestin_inscription.services.serviceImpl;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import com.groupe.gestin_inscription.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import java.io.File;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.image.GrayU8;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;



@Service
@RequiredArgsConstructor
public class DocumentManagerService {

    private final Path secureStoragePath = Paths.get("path/to/your/secure/storage");
    private final DocumentRepository documentRepository;

    /**
     * Verifies the file format and size based on the document type.
     *
     * @param documentType The type of document being uploaded (e.g., "Baccalauréat").
     * @param file The MultipartFile containing the file content.
     * @return True if the format and size are valid, false otherwise.
     */
    public boolean verifyFormat(String documentType, MultipartFile file) {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        long fileSize = file.getSize();

        // Check for general file size limit (max 5Mo)
        if (fileSize > 5 * 1024 * 1024) {
            return false;
        }

        // Specific format checks based on the document type
        switch (documentType) {
            case "Diplômes": // Baccalauréat, Diplômes supérieurs
                return "pdf".equalsIgnoreCase(fileExtension);
            case "CNI recto/verso":
                return "jpg".equalsIgnoreCase(fileExtension) || "png".equalsIgnoreCase(fileExtension);
            case "Acte de naissance":
                return "pdf".equalsIgnoreCase(fileExtension);
            case "Photo d'identité":
                return "jpg".equalsIgnoreCase(fileExtension) || "png".equalsIgnoreCase(fileExtension);
            case "Relevés de notes": // For partial OCR
                return "pdf".equalsIgnoreCase(fileExtension) || "jpg".equalsIgnoreCase(fileExtension) || "png".equalsIgnoreCase(fileExtension);
            default:
                // For any other document type, assume PDF is the default
                return "pdf".equalsIgnoreCase(fileExtension);
        }
    }

    /**
     * Saves the uploaded file to a secure, persistent storage location.
     *
     * @param file The MultipartFile to save.
     * @return The secure path to the saved file.
     * @throws IOException if there's an error saving the file.
     */
    public String saveSecurely(MultipartFile file) throws IOException {
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path destinationFile = this.secureStoragePath.resolve(Paths.get(uniqueFileName));

        // Ensure the directory exists
        Files.createDirectories(destinationFile.getParent());

        Files.copy(file.getInputStream(), destinationFile);

        return destinationFile.toString();
    }

    /**
     * Performs partial OCR on specific documents like academic transcripts ("relevés de notes").
     * @param filePath The path to the document file.
     * @return True if the OCR check is successful, false otherwise.
     */
    public boolean performOcrCheck(String filePath) {
        // 1. Initialize the OCR engine
        Tesseract tesseract = new Tesseract();
        // Path to the Tesseract data folder (tessdata)
        // Tesseract.setDatapath("/path/to/tessdata");

        // 2. Read the image/PDF file
        File documentFile = new File(filePath);

        if (!documentFile.exists()) {
            System.err.println("File not found at: " + filePath);
            return false;
        }

        try {
            // 3. Call the OCR engine to extract text
            String extractedText = tesseract.doOCR(documentFile);

            // 4. Analyze the extracted text to verify key information
            // For an academic transcript, you might look for keywords like "Baccalauréat" or "Spécialisation" [cite: 54, 58]
            // and check for the presence of a certain graduation year or academic record.
            if (extractedText.contains("relevé de notes") || extractedText.contains("Baccalauréat")) {
                System.out.println("Partial OCR successful. Document identified as an academic record.");
                return true;
            } else {
                System.out.println("Partial OCR failed. Key keywords not found.");
                return false;
            }
        } catch (TesseractException e) {
            System.err.println("Error during OCR processing: " + e.getMessage());
            return false;
        }
    }

    /**
     * Detects watermarks on documents like birth certificates ("Acte de naissance").
     *
     * @param filePath     The path to the document file.
     * @param fileType
     * @return True if a watermark is detected, false otherwise.
     */
    public boolean detectWatermark(String filePath, String fileType) {
        // Step 1: Read the image file from the specified path
        BufferedImage originalImage;
        originalImage = UtilImageIO.loadImage(String.valueOf(new File(filePath)));
        if (originalImage == null) {
            System.err.println("Error: Could not read image from file path.");
            return false;
        }

        // Step 2: Convert the image to a grayscale format for processing
        GrayF32 grayImage = ConvertBufferedImage.convertFromSingle(originalImage,null, GrayF32.class);

        // Step 3: Apply a Gaussian blur to smooth the image and remove noise
        GrayF32 blurredImage = new GrayF32(grayImage.width, grayImage.height);
        BlurImageOps.gaussian(grayImage, blurredImage, -1, 2, null);

        // Step 4: Detect the watermark by applying a local threshold
        // This is a common technique to segment out faint, low-contrast patterns like watermarks.
        GrayU8 binaryImage = new GrayU8(grayImage.width, grayImage.height);
        GThresholdImageOps.localMean(blurredImage, binaryImage, ConfigLength.fixed(15), 0.05F, true, null, null, null);

        // Step 5: Count the number of white pixels (potential watermark regions)
        //int whitePixelCount = ImageMiscOps.count(binaryImage, 255);
        //alternative to the .count()
        int whitePixelCount = 0;
        for (int y = 0; y < binaryImage.height; y++) {
            for (int x = 0; x < binaryImage.width; x++) {
                if (binaryImage.get(x, y) == 255) {
                    whitePixelCount++;
                }
            }
        }

        // Step 6: Define a heuristic to determine if a watermark exists
        // This is a simple heuristic; more complex logic might be needed for a robust solution.
        // If the number of white pixels exceeds a certain percentage of the total image area,
        // it's likely a watermark.
//        double totalPixels = grayImage.width * grayImage.height;
//        double watermarkPercentage = (double) whitePixelCount / totalPixels;
//        double watermarkThreshold = 0.05; // 5% of the image area
//        System.out.println("White pixel percentage: " + (watermarkPercentage * 100) + "%");
        // Step 6: Define a HEURISTIC using document type
        double watermarkThreshold = 0.05; // Default to 5%

        // Use a switch or if/else structure to set the threshold dynamically
        if ("Acte de naissance".equalsIgnoreCase(fileType)) {
            // Birth certificates often have a clear, large seal/watermark, requiring a lower threshold.
            watermarkThreshold = 0.02; // Only 2% coverage needed to confirm presence.
        } else if ("Diplôme".equalsIgnoreCase(fileType)) {
            // Diplomas might have a protective pattern, requiring a higher threshold.
            watermarkThreshold = 0.08;
        }


        // Step 7: Final calculation
        double totalPixels = grayImage.width * grayImage.height;
        double watermarkPercentage = (double) whitePixelCount / totalPixels;

        if (watermarkPercentage > watermarkThreshold) {
            System.out.println("Watermark detected.");
            return true;
        } else {
            System.out.println("No watermark detected.");
            return false;
        }
    }

    /**
     * Verifies the ratio and facial presence in an ID photo.
     *
     * @param filePath The path to the ID photo.
     * @return True if the photo meets the criteria, false otherwise.
     */
    public boolean verifyPhotoRatio(String filePath) {
        try {
            // Step 1: Load the image from the file path
            File imageFile = new File(filePath);
            if (!imageFile.exists()) {
                System.err.println("File not found at: " + filePath);
                return false;
            }
            BufferedImage image = ImageIO.read(imageFile);

            if (image == null) {
                System.err.println("Could not read image file at: " + filePath);
                return false;
            }

            // Step 2: Check if the ratio is close to 3.5x4.5cm.
            double width = image.getWidth();
            double height = image.getHeight();
            double aspectRatio = width / height;
            double targetRatio = 3.5 / 4.5;

            // Allowing a small tolerance for the ratio check
            double tolerance = 0.05; // 5% tolerance
            if (Math.abs(aspectRatio - targetRatio) > tolerance) {
                System.out.println("Photo ratio verification failed. Actual ratio: " + aspectRatio);
                return false;
            }

            // Step 3: Use a facial recognition library to verify a face is present.
            // This part requires a facial recognition library like OpenCV
            // Make sure the native OpenCV library is loaded at application startup (e.g., in main method)
            // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            // Load the pre-trained Haar Cascade classifier for face detection.
            // The .xml file must be in your resources folder or specified with a full path.
            Mat imageMat = Imgcodecs.imread(filePath);
            CascadeClassifier faceDetector = new CascadeClassifier(getClass().getResource("/haarcascade_frontalface_alt.xml").getPath());
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(imageMat, faceDetections);
            if (faceDetections.toArray().length == 0) {
                 System.out.println("No face detected in the photo.");
                 return false;
             }

            System.out.println("Photo ratio and face detection passed for file at: " + filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks for the similarity of a new document by comparing its hash
     * with the hashes of existing documents in the database.
     * This method is a form of elementary fraud detection.
     *
     * @param filePath The path to the new document file.
     * @return true if a similar document is found, false otherwise.
     */

    public boolean checkForSimilarity(String filePath) {
        // Step 1: Generate the hash for the new document
        String newDocumentHash = generateFileHash(filePath);
        if (newDocumentHash == null) {
            // Handle error (e.g., file not found or hashing failed)
            return false;
        }

        // Step 2: Query the database for a document with the same hash
        boolean isDuplicate = documentRepository.findByHash(newDocumentHash).isPresent();

        if (isDuplicate) {
            System.out.println("ALERT: Similar document detected for file at: " + filePath);
        }

        return isDuplicate;
    }


     //Helper method to generate a SHA-256 hash of a file.

    private String generateFileHash(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashedBytes = digest.digest();
            return new BigInteger(1, hashedBytes).toString(16);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error generating file hash: " + e.getMessage());
            return null;
        }
    }

    String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}
