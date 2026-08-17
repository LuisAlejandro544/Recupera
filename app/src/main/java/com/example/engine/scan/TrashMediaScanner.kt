package com.example.engine.scan

import android.content.Context
import android.provider.MediaStore
import com.example.engine.scan.trash.MediaStoreTrashQueryHelper
import com.example.model.MediaType
import com.example.model.RecoverablePhoto

object TrashMediaScanner {

    fun scanMediaStoreTrash(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        MediaStoreTrashQueryHelper.queryTrashedMedia(
            context = context,
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            idPrefix = "trash_img",
            defaultNamePrefix = "Foto_Papelera",
            defaultExt = "jpg",
            defaultSize = 512 * 1024L,
            foundPhotos = foundPhotos,
            seenPaths = seenPaths
        )
    }

    fun scanMediaStoreVideoTrash(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        MediaStoreTrashQueryHelper.queryTrashedMedia(
            context = context,
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.VIDEO,
            idPrefix = "trash_vid",
            defaultNamePrefix = "Video_Papelera",
            defaultExt = "mp4",
            defaultSize = 2 * 1024 * 1024L,
            foundPhotos = foundPhotos,
            seenPaths = seenPaths
        )
    }

    fun scanMediaStoreAudioTrash(
        context: Context,
        foundPhotos: MutableList<RecoverablePhoto>,
        seenPaths: MutableSet<String>
    ) {
        MediaStoreTrashQueryHelper.queryTrashedMedia(
            context = context,
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.AUDIO,
            idPrefix = "trash_aud",
            defaultNamePrefix = "Audio_Papelera",
            defaultExt = "mp3",
            defaultSize = 500 * 1024L,
            foundPhotos = foundPhotos,
            seenPaths = seenPaths
        )
    }
}
