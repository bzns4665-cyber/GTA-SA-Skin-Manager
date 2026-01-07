package com.gtasa.skinmanager;

import android.content.Context;
import android.net.Uri;
import java.io.*;

public class FileUtils {
    
    /**
     * Read file content from URI
     */
    public static byte[] readFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Cannot open input stream for URI: " + uri);
        }
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        inputStream.close();
        return buffer.toByteArray();
    }
    
    /**
     * Write data to file
     */
    public static boolean writeFile(File file, byte[] data) {
        try {
            // Create parent directories if needed
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Read file content
     */
    public static byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        byte[] data = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = fis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        fis.close();
        return buffer.toByteArray();
    }
    
    /**
     * Copy file
     */
    public static boolean copyFile(File source, File destination) {
        try {
            byte[] data = readFile(source);
            return writeFile(destination, data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if file exists and is readable
     */
    public static boolean isFileAccessible(File file) {
        return file != null && file.exists() && file.canRead();
    }
    
    /**
     * Get file extension
     */
    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Format file size for display
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}