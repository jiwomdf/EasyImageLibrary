package com.programmergabut.imageharpa.manage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.programmergabut.imageharpa.ImageHarpa.Companion.TAG
import com.programmergabut.imageharpa.domain.ManageImage
import com.programmergabut.imageharpa.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ManageImageImpl(
    private val context: Context,
    private val fileName: String,
    directory: String?,
    private val fileExtension: Extension,
    env: String = Environment.DIRECTORY_PICTURES
): ManageImage {

    /**
     * For internal image
     */
    private val absolutePath = context.getExternalFilesDir(env)?.absolutePath
    private val finalDirectory = if(directory.isNullOrEmpty()) "" else directory.trim()

    /**
     * For public image
     */
    private val collection = sdk29AndUp { MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) }
        ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,)
    private val where = MediaStore.Images.Media.DISPLAY_NAME + " LIKE " + "'%$fileName${setExtension(fileExtension)}%'"

    override fun load(): Bitmap? {
        return try {
            validateFileName(fileName)
            validateReadPermission(context)
            val extension = setExtension(fileExtension)
            val directory = File("${absolutePath}${File.separator}$finalDirectory")
            validateDirectory(directory)

            val file = File(directory, "$fileName$extension")
            BitmapFactory.decodeFile(file.path)
        } catch (ex: Exception){
            Log.e(TAG, "load: ${ex.message}")
            null
        }
    }

    override fun load(callBack: LoadImageCallback){
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateFileName(fileName)
                validateReadPermission(context)
                val extension = setExtension(fileExtension)
                val directory = File("${absolutePath}${File.separator}$finalDirectory")
                validateDirectory(directory)

                val file = File(directory, "$fileName$extension")
                val result = BitmapFactory.decodeFile(file.path)
                withContext(Dispatchers.Main){
                    callBack.onResult(result)
                }
            } catch (ex: Exception){
                Log.e(TAG, "load: ${ex.message}")
                withContext(Dispatchers.Main){
                    callBack.onResult(null)
                }
            }
        }
    }

    override fun delete(): Boolean {
        return try {
            val extension = setExtension(fileExtension)
            val directory = File("${absolutePath}${File.separator}$finalDirectory")
            validateDirectory(directory)

            val file = File(directory, "$fileName$extension")
            file.delete()
        } catch (ex: Exception){
            Log.e(TAG, "delete: ${ex.message}")
            true
        }
    }

    override fun delete(callBack: ImageCallback){
        CoroutineScope(Dispatchers.Default).launch {
            val extension = setExtension(fileExtension)
            val directory = File("${absolutePath}${File.separator}$finalDirectory")
            validateDirectory(directory)

            try {
                val file = File(directory, "$fileName$extension")
                if(file.delete()){
                    withContext(Dispatchers.Main) { callBack.onSuccess() }
                } else {
                    throw Exception("file not deleted")
                }
            } catch (ex: Exception){
                Log.e(TAG, "delete: ${ex.message}")
                withContext(Dispatchers.Main) {
                    callBack.onFailed(ex)
                }
            }
        }
    }

    override fun save(bitmap: Bitmap, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateStoragePermission(context)
            val extension = setExtension(fileExtension)
            val expectedDir = File("${absolutePath}${File.separator}$finalDirectory")
            val directory = getOrCreateDirectoryIfEmpty(expectedDir)

            val file = File(directory, "$fileName$extension")
            deleteFileIfExist(file)
            val outputStream = FileOutputStream(file)
            compressBitmap(outputStream, bitmap, quality, fileExtension)
            true
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun save(bitmap: Bitmap, quality: Int, imageCallBack: ImageCallback){
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateStoragePermission(context)
                val extension = setExtension(fileExtension)
                val expectedDir = File("${absolutePath}${File.separator}$finalDirectory")
                val directory = getOrCreateDirectoryIfEmpty(expectedDir)

                val file = File(directory, "$fileName$extension")
                deleteFileIfExist(file)
                kotlin.runCatching {
                    val outputStream = FileOutputStream(file)
                    compressBitmap(outputStream, bitmap, quality, fileExtension)
                }
                withContext(Dispatchers.Main){
                    imageCallBack.onSuccess()
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    imageCallBack.onFailed(ex)
                }
            }
        }

    }

    override fun save(base64: String, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateBase64String(base64)
            validateStoragePermission(context)
            val bitmap = decodeByteArray(base64)
            val extension = setExtension(fileExtension)
            val expectedDir = File("${absolutePath}${File.separator}$finalDirectory")
            val directory = getOrCreateDirectoryIfEmpty(expectedDir)

            val file = File(directory, "$fileName$extension")
            deleteFileIfExist(file)
            val outputStream = FileOutputStream(file)
            compressBitmap(outputStream, bitmap, quality, fileExtension)
            true
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun save(base64: String, quality: Int, imageCallBack: ImageCallback){
        CoroutineScope(Dispatchers.Default).launch{
            try {
                validateImageQuality(quality)
                validateBase64String(base64)
                validateStoragePermission(context)
                val bitmap = decodeByteArray(base64)
                val extension = setExtension(fileExtension)
                val expectedDir = File("${absolutePath}${File.separator}$finalDirectory")
                val directory = getOrCreateDirectoryIfEmpty(expectedDir)

                val file = File(directory, "$fileName$extension")
                deleteFileIfExist(file)
                kotlin.runCatching {
                    val outputStream = FileOutputStream(file)
                    compressBitmap(outputStream, bitmap, quality, fileExtension)
                }
                withContext(Dispatchers.Main){
                    imageCallBack.onSuccess()
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    imageCallBack.onFailed(ex)
                }
            }
        }
    }

    override fun save(drawable: Drawable, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateStoragePermission(context)
            val bitmap = drawableToBitmap(drawable)
            val extension = setExtension(fileExtension)
            val expectedDir = File("${absolutePath}${File.separator}$finalDirectory")
            val directory = getOrCreateDirectoryIfEmpty(expectedDir)

            val file = File(directory, "$fileName$extension")
            deleteFileIfExist(file)
            val outputStream = FileOutputStream(file)
            compressBitmap(outputStream, bitmap, quality, fileExtension)
            true
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun save(drawable: Drawable, quality: Int,
                      imageCallBack: ImageCallback
    ){
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateStoragePermission(context)
                val bitmap = drawableToBitmap(drawable)
                val extension = setExtension(fileExtension)
                val expectedDir = File("${absolutePath}${File.separator}$finalDirectory")
                val directory = getOrCreateDirectoryIfEmpty(expectedDir)

                val file = File(directory, "$fileName$extension")
                deleteFileIfExist(file)
                kotlin.runCatching {
                    val outputStream = FileOutputStream(file)
                    compressBitmap(outputStream, bitmap, quality, fileExtension)
                }
                withContext(Dispatchers.Main){
                    imageCallBack.onSuccess()
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    imageCallBack.onFailed(ex)
                }
            }
        }
    }

    override fun loadPublic(): Bitmap? {
        return try {
            validateFileName(fileName)
            validateReadPermission(context)
            val photoUri = loadPublicPhotoUri(context, fileName, collection, projection, where) ?: throw Exception("cant get photo Uri")
            loadBitmapFromUri(context, photoUri)
        } catch (ex: Exception){
            Log.e(TAG, "loadPublic: ${ex.message}", )
            null
        }
    }

    override fun loadPublic(callBack: LoadImageCallback) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateFileName(fileName)
                validateReadPermission(context)
                val photoUri = loadPublicPhotoUri(context, fileName, collection, projection, where) ?: throw Exception("cant get photo Uri")
                withContext(Dispatchers.Main){
                    callBack.onResult(loadBitmapFromUri(context, photoUri))
                }
            } catch (ex: Exception){
                Log.e(TAG, "loadPublic: ${ex.message}", )
                withContext(Dispatchers.Main){
                    callBack.onFailed(ex)
                }
            }
        }
    }

    override fun loadPublicUri(): Uri? {
        return try {
            validateFileName(fileName)
            validateReadPermission(context)
            loadPublicPhotoUri(context, fileName, collection, projection, where) ?: throw Exception("cant get photo Uri")
        } catch (ex: Exception){
            Log.e(TAG, "loadPublicUri: ${ex.message}", )
            null
        }
    }

    override fun savePublic(bitmap: Bitmap, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateStoragePermission(context)
            val env = Environment.DIRECTORY_DCIM
            val directory = "${env}${File.separator}$finalDirectory"
            val expectedDir = File(directory)
            getOrCreateDirectoryIfEmpty(expectedDir)
            savePublicImage(context, bitmap, directory, quality, fileName, fileExtension)
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun savePublic(bitmap: Bitmap, quality: Int, imageCallBack: ImageCallback) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateStoragePermission(context)
                val directory = "${Environment.DIRECTORY_DCIM}${File.separator}$finalDirectory"
                val expectedDir = File(directory)
                getOrCreateDirectoryIfEmpty(expectedDir)
                val isSuccess = savePublicImage(context, bitmap, directory, quality, fileName, fileExtension)
                withContext(Dispatchers.Main){
                    if(isSuccess) {
                        imageCallBack.onSuccess()
                    } else {
                        imageCallBack.onFailed(Exception())
                    }
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    imageCallBack.onFailed(ex)
                }
            }
        }
    }

    override fun savePublic(base64: String, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateBase64String(base64)
            validateStoragePermission(context)
            val bitmap = decodeByteArray(base64)
            val directory = "${Environment.DIRECTORY_DCIM}${File.separator}$finalDirectory${File.separator}"
            val expectedDir = File(directory)
            getOrCreateDirectoryIfEmpty(expectedDir)
            savePublicImage(context, bitmap, directory, quality, fileName, fileExtension)
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun savePublic(base64: String, quality: Int, imageCallBack: ImageCallback) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateBase64String(base64)
                validateStoragePermission(context)
                val bitmap = decodeByteArray(base64)
                val directory = "${Environment.DIRECTORY_DCIM}${File.separator}$finalDirectory${File.separator}"
                val expectedDir = File(directory)
                getOrCreateDirectoryIfEmpty(expectedDir)
                val isSuccess = savePublicImage(context, bitmap, directory, quality, fileName, fileExtension)
                withContext(Dispatchers.Main){
                    if(isSuccess) {
                        imageCallBack.onSuccess()
                    } else {
                        imageCallBack.onFailed(Exception())
                    }
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    imageCallBack.onFailed(ex)
                }
            }
        }
    }

    override fun savePublic(drawable: Drawable, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateStoragePermission(context)
            val directory = "${Environment.DIRECTORY_DCIM}${File.separator}$finalDirectory${File.separator}"
            val expectedDir = File(directory)
            getOrCreateDirectoryIfEmpty(expectedDir)
            val bitmap = drawableToBitmap(drawable)
            savePublicImage(context, bitmap, directory, quality, fileName, fileExtension)
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun savePublic(drawable: Drawable, quality: Int, imageCallBack: ImageCallback) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateStoragePermission(context)
                val directory = "${Environment.DIRECTORY_DCIM}${File.separator}$finalDirectory${File.separator}"
                val expectedDir = File(directory)
                getOrCreateDirectoryIfEmpty(expectedDir)
                val bitmap = drawableToBitmap(drawable)
                val isSuccess = savePublicImage(context, bitmap, directory, quality, fileName, fileExtension)
                withContext(Dispatchers.Main){
                    if(isSuccess) {
                        imageCallBack.onSuccess()
                    } else {
                        imageCallBack.onFailed(Exception())
                    }
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    imageCallBack.onFailed(ex)
                }
            }
        }
    }


}

