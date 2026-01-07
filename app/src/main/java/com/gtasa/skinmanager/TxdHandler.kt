package com.gtasa.skinmanager

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * معالج بسيط لملفات TXD (RenderWare Texture Dictionary)
 * يدعم العمليات الأساسية للاستبدال
 */
class TxdHandler {
    
    companion object {
        /**
         * نسخ ملف TXD إلى مجلد texdb
         */
        fun replaceTxdFile(texdbFolder: File, characterName: String, newTxdFile: File): Boolean {
            if (!texdbFolder.exists() || !texdbFolder.isDirectory) {
                return false
            }
            
            if (!newTxdFile.exists()) {
                return false
            }

            // البحث عن ملف TXD الموجود
            val txdFileName = "$characterName.txd"
            val targetTxdFile = File(texdbFolder, txdFileName)

            return try {
                // نسخ الملف الجديد ليحل محل القديم
                newTxdFile.copyTo(targetTxdFile, overwrite = true)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        /**
         * التحقق من صحة ملف TXD
         */
        fun isValidTxdFile(file: File): Boolean {
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
                    
                    // RenderWare Texture Dictionary section type is 0x16
                    sectionType == 0x16
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}