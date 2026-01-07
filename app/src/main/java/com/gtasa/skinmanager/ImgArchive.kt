package com.gtasa.skinmanager

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * معالج ملفات IMG Archive (gta3.img)
 * يدعم IMG Version 2 المستخدم في GTA San Andreas
 */
class ImgArchive(private val imgFile: File) {
    
    companion object {
        private const val SECTOR_SIZE = 2048
        private const val ENTRY_SIZE = 32
        private const val VERSION_MARKER = "VER2"
    }

    data class ImgEntry(
        var offset: Int,
        var streamingSize: Short,
        var archiveSize: Short,
        var fileName: String
    )

    private var entries = mutableListOf<ImgEntry>()
    private var numEntries = 0

    /**
     * قراءة وتحليل ملف IMG
     */
    fun read(): Boolean {
        if (!imgFile.exists()) return false

        RandomAccessFile(imgFile, "r").use { raf ->
            // قراءة الهيدر (8 bytes)
            val headerBytes = ByteArray(8)
            raf.read(headerBytes)
            
            val version = String(headerBytes, 0, 4, StandardCharsets.US_ASCII)
            if (version != VERSION_MARKER) {
                return false
            }

            val buffer = ByteBuffer.wrap(headerBytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(4)
            numEntries = buffer.int

            // قراءة Directory Entries
            entries.clear()
            for (i in 0 until numEntries) {
                val entryBytes = ByteArray(ENTRY_SIZE)
                raf.read(entryBytes)
                
                val entryBuffer = ByteBuffer.wrap(entryBytes)
                entryBuffer.order(ByteOrder.LITTLE_ENDIAN)

                val offset = entryBuffer.int
                val streamingSize = entryBuffer.short
                val archiveSize = entryBuffer.short
                
                val nameBytes = ByteArray(24)
                entryBuffer.get(nameBytes)
                val fileName = String(nameBytes, StandardCharsets.US_ASCII).trim('\u0000')

                if (fileName.isNotEmpty()) {
                    entries.add(ImgEntry(offset, streamingSize, archiveSize, fileName))
                }
            }
        }
        return true
    }

    /**
     * استخراج ملف من الأرشيف
     */
    fun extractFile(fileName: String, outputFile: File): Boolean {
        val entry = entries.find { it.fileName.equals(fileName, ignoreCase = true) } ?: return false

        RandomAccessFile(imgFile, "r").use { raf ->
            val dataOffset = (entry.offset * SECTOR_SIZE).toLong()
            val fileSize = (entry.streamingSize * SECTOR_SIZE).toLong()

            raf.seek(dataOffset)
            val data = ByteArray(fileSize.toInt())
            raf.read(data)

            outputFile.writeBytes(data)
        }
        return true
    }

    /**
     * استبدال ملف في الأرشيف
     */
    fun replaceFile(fileName: String, newFile: File): Boolean {
        val entry = entries.find { it.fileName.equals(fileName, ignoreCase = true) } ?: return false
        
        val newData = newFile.readBytes()
        val newSizeInSectors = ((newData.size + SECTOR_SIZE - 1) / SECTOR_SIZE).toShort()

        RandomAccessFile(imgFile, "rw").use { raf ->
            val dataOffset = (entry.offset * SECTOR_SIZE).toLong()
            raf.seek(dataOffset)
            
            // كتابة البيانات الجديدة
            raf.write(newData)
            
            // ملء باقي القطاع بأصفار
            val padding = (newSizeInSectors * SECTOR_SIZE) - newData.size
            if (padding > 0) {
                raf.write(ByteArray(padding))
            }

            // تحديث directory entry
            entry.streamingSize = newSizeInSectors
            entry.archiveSize = newSizeInSectors
            
            val entryIndex = entries.indexOf(entry)
            val entryPosition = 8L + (entryIndex * ENTRY_SIZE) + 4 // skip offset field
            raf.seek(entryPosition)
            
            val buffer = ByteBuffer.allocate(4)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putShort(newSizeInSectors)
            buffer.putShort(newSizeInSectors)
            raf.write(buffer.array())
        }
        return true
    }

    /**
     * الحصول على قائمة بأسماء الملفات
     */
    fun getFileList(): List<String> {
        return entries.map { it.fileName }
    }

    /**
     * التحقق من وجود ملف
     */
    fun fileExists(fileName: String): Boolean {
        return entries.any { it.fileName.equals(fileName, ignoreCase = true) }
    }
}