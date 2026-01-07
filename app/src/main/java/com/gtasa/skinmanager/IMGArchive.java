package com.gtasa.skinmanager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for GTA SA IMG Archive (Version 2)
 * Based on IMG Archive format documentation
 */
public class IMGArchive {
    
    private static final int SECTOR_SIZE = 2048;
    private static final int ENTRY_SIZE = 32;
    private static final String VERSION_TAG = "VER2";
    
    private File imgFile;
    private List<IMGEntry> entries;
    private int fileCount;
    
    public static class IMGEntry {
        public int offset;        // Offset in sectors (2048 bytes each)
        public int streamingSize; // Size for streaming
        public int sizeInArchive; // Size in sectors
        public String fileName;   // 24 bytes max
        
        public IMGEntry(int offset, int streamingSize, int sizeInArchive, String fileName) {
            this.offset = offset;
            this.streamingSize = streamingSize;
            this.sizeInArchive = sizeInArchive;
            this.fileName = fileName.toLowerCase();
        }
        
        public int getAbsoluteOffset() {
            return offset * SECTOR_SIZE;
        }
        
        public int getDataSize() {
            return sizeInArchive * SECTOR_SIZE;
        }
    }
    
    public IMGArchive(File imgFile) throws IOException {
        this.imgFile = imgFile;
        this.entries = new ArrayList<>();
        readArchive();
    }
    
    private void readArchive() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(imgFile, "r")) {
            // Read header
            byte[] header = new byte[8];
            raf.read(header);
            
            String version = new String(header, 0, 4);
            if (!version.equals(VERSION_TAG)) {
                throw new IOException("Invalid IMG archive version: " + version);
            }
            
            // Read file count
            fileCount = ByteBuffer.wrap(header, 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();
            
            // Read directory entries
            for (int i = 0; i < fileCount; i++) {
                byte[] entryData = new byte[ENTRY_SIZE];
                raf.read(entryData);
                
                ByteBuffer buffer = ByteBuffer.wrap(entryData).order(ByteOrder.LITTLE_ENDIAN);
                
                int offset = buffer.getInt();
                int streamingSize = buffer.getShort() & 0xFFFF;
                int sizeInArchive = buffer.getShort() & 0xFFFF;
                
                // Read file name (24 bytes)
                byte[] nameBytes = new byte[24];
                buffer.get(nameBytes);
                String fileName = new String(nameBytes).trim().replace("\0", "");
                
                entries.add(new IMGEntry(offset, streamingSize, sizeInArchive, fileName));
            }
        }
    }
    
    public boolean replaceFile(String fileName, byte[] newData) {
        try {
            fileName = fileName.toLowerCase();
            
            // Find the entry
            IMGEntry targetEntry = null;
            for (IMGEntry entry : entries) {
                if (entry.fileName.equalsIgnoreCase(fileName)) {
                    targetEntry = entry;
                    break;
                }
            }
            
            if (targetEntry == null) {
                System.err.println("File not found in archive: " + fileName);
                return false;
            }
            
            // Calculate required sectors
            int requiredSectors = (int) Math.ceil((double) newData.length / SECTOR_SIZE);
            
            // Pad data to sector boundary
            byte[] paddedData = new byte[requiredSectors * SECTOR_SIZE];
            System.arraycopy(newData, 0, paddedData, 0, newData.length);
            
            // Write to archive
            try (RandomAccessFile raf = new RandomAccessFile(imgFile, "rw")) {
                long writePosition = (long) targetEntry.offset * SECTOR_SIZE;
                raf.seek(writePosition);
                raf.write(paddedData);
                
                // Update entry size if needed
                if (requiredSectors != targetEntry.sizeInArchive) {
                    targetEntry.sizeInArchive = requiredSectors;
                    targetEntry.streamingSize = requiredSectors;
                    updateDirectoryEntry(raf, targetEntry);
                }
            }
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void updateDirectoryEntry(RandomAccessFile raf, IMGEntry entry) throws IOException {
        // Find entry position in directory
        int entryIndex = entries.indexOf(entry);
        long entryPosition = 8 + (entryIndex * ENTRY_SIZE);
        
        raf.seek(entryPosition);
        
        ByteBuffer buffer = ByteBuffer.allocate(ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(entry.offset);
        buffer.putShort((short) entry.streamingSize);
        buffer.putShort((short) entry.sizeInArchive);
        
        byte[] nameBytes = new byte[24];
        byte[] nameData = entry.fileName.getBytes();
        System.arraycopy(nameData, 0, nameBytes, 0, Math.min(nameData.length, 24));
        buffer.put(nameBytes);
        
        raf.write(buffer.array());
    }
    
    public byte[] extractFile(String fileName) throws IOException {
        fileName = fileName.toLowerCase();
        
        IMGEntry targetEntry = null;
        for (IMGEntry entry : entries) {
            if (entry.fileName.equalsIgnoreCase(fileName)) {
                targetEntry = entry;
                break;
            }
        }
        
        if (targetEntry == null) {
            throw new IOException("File not found: " + fileName);
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(imgFile, "r")) {
            long readPosition = (long) targetEntry.offset * SECTOR_SIZE;
            int dataSize = targetEntry.sizeInArchive * SECTOR_SIZE;
            
            byte[] data = new byte[dataSize];
            raf.seek(readPosition);
            raf.read(data);
            
            return data;
        }
    }
    
    public List<String> listFiles() {
        List<String> fileNames = new ArrayList<>();
        for (IMGEntry entry : entries) {
            fileNames.add(entry.fileName);
        }
        return fileNames;
    }
    
    public int getFileCount() {
        return fileCount;
    }
}