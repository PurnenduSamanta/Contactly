package com.purnendu.contactly.domain.repository

interface ImageStorageRepository {
    fun saveTemporaryImage(activationId: Long, image: String): String?
    fun saveOriginalImage(activationId: Long, contactId: Long): String?
    fun deleteImagesFromActivation(activationId: Long)
}
