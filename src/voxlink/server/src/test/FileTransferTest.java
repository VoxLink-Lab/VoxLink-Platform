package voxlink.server.src.test;

import voxlink.shared.protocol.FileTransferProtocol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * FileTransferTest verifies file transfer functionality.
 * Tests chunk size calculations, file validation, and temp file management.
 */
public class FileTransferTest {

    private static final String TEST_DIR = "./test_files/";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("VoxLink File Transfer Test Suite");
        System.out.println("=========================================\n");

        // Create test directory
        new File(TEST_DIR).mkdirs();

        boolean allTestsPassed = true;

        allTestsPassed &= testChunkCalculation();
        allTestsPassed &= testFileTypeValidation();
        allTestsPassed &= testFileNameGeneration();
        allTestsPassed &= testFileSizeValidation();
        allTestsPassed &= testTempFileManagement();

        // Cleanup
        deleteTestFiles();

        System.out.println("\n=========================================");
        if (allTestsPassed) {
            System.out.println("✅ ALL FILE TRANSFER TESTS PASSED!");
        } else {
            System.out.println("❌ SOME TESTS FAILED - Check logs above");
        }
        System.out.println("=========================================");
    }

    /**
     * Test 1: Chunk Size Calculation
     */
    private static boolean testChunkCalculation() {
        System.out.println("\n📋 Test 1: Chunk Size Calculation");

        // Test with exact chunk size
        int chunks1 = FileTransferProtocol.calculateChunkCount(64 * 1024);
        if (chunks1 != 1) {
            System.out.println("   ❌ 64KB file should be 1 chunk, got " + chunks1);
            return false;
        }

        // Test with 1 byte over
        int chunks2 = FileTransferProtocol.calculateChunkCount(64 * 1024 + 1);
        if (chunks2 != 2) {
            System.out.println("   ❌ 64KB+1 file should be 2 chunks, got " + chunks2);
            return false;
        }

        // Test with large file
        int chunks3 = FileTransferProtocol.calculateChunkCount(100 * 1024 * 1024);
        int expected = (int) Math.ceil((100.0 * 1024 * 1024) / (64.0 * 1024));
        if (chunks3 != expected) {
            System.out.println("   ❌ 100MB file chunk calculation wrong");
            return false;
        }

        System.out.println("   ✅ Chunk calculation correct (64KB chunks)");
        return true;
    }

    /**
     * Test 2: File Type Validation
     */
    private static boolean testFileTypeValidation() {
        System.out.println("\n📋 Test 2: File Type Validation");

        // Test allowed types
        String[] allowedTypes = {"image/jpeg", "image/png", "application/pdf", "text/plain"};
        for (String type : allowedTypes) {
            if (!FileTransferProtocol.isAllowedFileType(type)) {
                System.out.println("   ❌ " + type + " should be allowed");
                return false;
            }
        }

        // Test disallowed types
        String[] disallowedTypes = {"application/exe", "video/mp4", "audio/mp3"};
        for (String type : disallowedTypes) {
            if (FileTransferProtocol.isAllowedFileType(type)) {
                System.out.println("   ❌ " + type + " should NOT be allowed");
                return false;
            }
        }

        System.out.println("   ✅ File type validation correct");
        return true;
    }

    /**
     * Test 3: File Name Generation
     */
    private static boolean testFileNameGeneration() {
        System.out.println("\n📋 Test 3: File Name Generation");

        String originalName = "testfile.txt";
        String uniqueName = FileTransferProtocol.generateUniqueFileName(originalName, 123, 1234567890L);

        if (!uniqueName.contains("123") || !uniqueName.endsWith(".txt")) {
            System.out.println("   ❌ Unique file name generation failed: " + uniqueName);
            return false;
        }

        // Test without extension
        String noExt = FileTransferProtocol.getFileExtension("README");
        if (!noExt.isEmpty()) {
            System.out.println("   ❌ File without extension should return empty string");
            return false;
        }

        // Test with extension
        String withExt = FileTransferProtocol.getFileExtension("document.pdf");
        if (!".pdf".equals(withExt)) {
            System.out.println("   ❌ Wrong extension: " + withExt);
            return false;
        }

        System.out.println("   ✅ File name generation correct");
        return true;
    }

    /**
     * Test 4: File Size Validation
     */
    private static boolean testFileSizeValidation() {
        System.out.println("\n📋 Test 4: File Size Validation");

        long maxSize = FileTransferProtocol.MAX_FILE_SIZE;

        if (maxSize != 100 * 1024 * 1024) {
            System.out.println("   ❌ Max file size should be 100MB");
            return false;
        }

        System.out.println("   ✅ Max file size: " + (maxSize / (1024 * 1024)) + "MB");
        return true;
    }

    /**
     * Test 5: Temp File Management
     */
    private static boolean testTempFileManagement() {
        System.out.println("\n📋 Test 5: Temp File Management");

        String tempFilePath = TEST_DIR + "test_upload.tmp";

        try {
            // Create temp file
            RandomAccessFile raf = new RandomAccessFile(tempFilePath, "rw");
            raf.setLength(1024); // 1KB file
            raf.close();

            // Verify file exists
            if (!Files.exists(Paths.get(tempFilePath))) {
                System.out.println("   ❌ Temp file not created");
                return false;
            }

            // Write test data
            FileOutputStream fos = new FileOutputStream(tempFilePath);
            byte[] testData = "Hello VoxLink!".getBytes();
            fos.write(testData);
            fos.close();

            // Verify content
            byte[] readData = Files.readAllBytes(Paths.get(tempFilePath));
            String content = new String(readData);
            if (!content.contains("VoxLink")) {
                System.out.println("   ❌ File content verification failed");
                return false;
            }

            // Delete temp file
            Files.deleteIfExists(Paths.get(tempFilePath));

            if (Files.exists(Paths.get(tempFilePath))) {
                System.out.println("   ❌ Temp file not deleted");
                return false;
            }

            System.out.println("   ✅ Temp file management working");
            return true;

        } catch (IOException e) {
            System.out.println("   ❌ Temp file test failed: " + e.getMessage());
            return false;
        }
    }

    private static void deleteTestFiles() {
        try {
            File dir = new File(TEST_DIR);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        } catch (Exception e) {
            // Ignore
        }
    }
}