package org.falaeapp.falae.storage

import android.content.Context
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Created by marcelo on 9/17/17.
 */
class FileHandler {

    private val PUBLIC_IMAGES_PATH = "public_images"

    fun createPublicFolder(context: Context?): File {
        val folder = File(context?.filesDir, PUBLIC_IMAGES_PATH)
        if (folder.exists().not()) {
            folder.mkdirs()
        }
        return folder
    }

    fun createUserFolder(context: Context?, folderName: String): File {
        val folder = File(context?.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun createImg(folder: File, fileName: String, imgSrc: String): File {
        val extension = MimeTypeMap.getFileExtensionFromUrl(imgSrc)
        return File(folder, "$fileName.$extension")
    }

    fun deleteUserFolder(context: Context, folderName: String) {
        val folder = File(context.filesDir, folderName)
        folder.deleteRecursively()
    }
}