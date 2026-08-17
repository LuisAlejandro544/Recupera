package com.example.engine.restore

import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.model.RecoverablePhoto

object MediaDestinationResolver {

    /**
     * Resolves the target relative path in public storage.
     */
    fun resolveRelativePath(photo: RecoverablePhoto): String {
        return when {
            photo.isVideo -> "${Environment.DIRECTORY_MOVIES}/Restored_Videos"
            photo.isAudio -> "${Environment.DIRECTORY_MUSIC}/Restored_Audio"
            else -> "${Environment.DIRECTORY_PICTURES}/Restored_Photos"
        }
    }

    /**
     * Resolves the MediaStore collection URI for insertion.
     */
    fun resolveTargetCollection(photo: RecoverablePhoto): Uri {
        return when {
            photo.isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            photo.isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    /**
     * Generates a clean filename prefixed with timestamp for uniqueness.
     */
    fun generateRestoredFileName(photo: RecoverablePhoto): String {
        val originalName = photo.name.removePrefix(".")
        val defaultExt = when {
            photo.isVideo -> "mp4"
            photo.isAudio -> "mp3"
            else -> "jpg"
        }
        val cleanName = if (!originalName.contains(".")) "$originalName.$defaultExt" else originalName
        return "Restaurado_${System.currentTimeMillis()}_$cleanName"
    }
}
