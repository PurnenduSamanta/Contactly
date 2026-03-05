package com.purnendu.contactly.data.utils

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

/**
 * Utility class to manage contact images stored in app's internal storage.
 * 
 * Images are stored with naming convention:
 * - Temporary images: temp_image_{activationId}.jpg
 * - Original images: original_image_{activationId}.jpg
 * 
 * This approach is more efficient than storing Base64 in the database.
 */
class ImageStorageManager(private val context: Context,) {
    
    private val TAG = "ImageStorageHelper"
    private val IMAGES_DIR = "contact_images"
    
    /**
     * Get the images directory, creating it if necessary
     */
    private fun getImagesDir(): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Save temporary image from gallery URI to internal storage
     * @return Internal storage file path, or null if failed
     */
    fun saveTemporaryImage(activationId: Long, image: String): String? {
        return try {
            val uri = image.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $image")
                return null
            }
            
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                Log.e(TAG, "Read 0 bytes from URI: $image")
                return null
            }
            
            val file = File(getImagesDir(), "temp_image_$activationId.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            
            Log.d(TAG, "Saved temporary image: ${file.absolutePath}, size: ${bytes.size}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save temporary image", e)
            null
        }
    }
    
    /**
     * Save original contact photo to internal storage
     * @return Internal storage file path, or null if no photo or failed
     */
    fun saveOriginalImage(activationId: Long, contactId: Long): String? {
        return try {
            val contactUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendPath(contactId.toString())
                .build()
            
            val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                contactUri,
                true // preferHighres
            )
            
            if (inputStream == null) {
                Log.d(TAG, "Contact has no photo: $contactId")
                return null
            }
            
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                Log.e(TAG, "Read 0 bytes from contact photo: $contactId")
                return null
            }
            
            val file = File(getImagesDir(), "original_image_$activationId.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            
            Log.d(TAG, "Saved original image: ${file.absolutePath}, size: ${bytes.size}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save original image", e)
            null
        }
    }

    /**
     * Delete images for an activation (cleanup when activation is deleted)
     */
    fun deleteImagesFromActivation(activationId: Long) {
        try {
            val dir = getImagesDir()
            val tempFile = File(dir, "temp_image_$activationId.jpg")
            val originalFile = File(dir, "original_image_$activationId.jpg")
            
            if (tempFile.exists()) {
                tempFile.delete()
                Log.d(TAG, "Deleted temporary image: ${tempFile.absolutePath}")
            }
            if (originalFile.exists()) {
                originalFile.delete()
                Log.d(TAG, "Deleted original image: ${originalFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete images for activation: $activationId", e)
        }
    }
}
