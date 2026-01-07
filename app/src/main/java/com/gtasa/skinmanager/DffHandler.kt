package com.gtasa.skinmanager

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * معالج بسيط لملفات DFF (RenderWare Model Files)
 */
class DffHandler {
    
    companion object {
        /**
         * التحقق من صحة ملف DFF
         */
        fun isValidDffFile(file: File): Boolean {
            if (!file.exists() || file.length() < 12) {
                return false
            }

            return try {
                RandomAccessFile(file, "r").use { raf ->
                    val header = ByteArray(4)
                    raf.read(header)
                    val buffer = ByteBuffer.wrap(header)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    val sectionType = buffer.int
                    
                    // RenderWare Clump section type is 0x10
                    sectionType == 0x10
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}