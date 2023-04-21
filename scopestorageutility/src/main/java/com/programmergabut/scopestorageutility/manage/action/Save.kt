package com.programmergabut.scopestorageutility.manage.action

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.programmergabut.scopestorageutility.AndroidImageUtil
import com.programmergabut.scopestorageutility.manage.callback.ImageCallback
import com.programmergabut.scopestorageutility.manage.action.base.BaseAction
import com.programmergabut.scopestorageutility.manage.callback.OutStreamCallback
import com.programmergabut.scopestorageutility.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class Save(
    context: Context,
    fileName: String,
    directory: String?,
    fileExtension: Extension.ExtensionModel,
    env: String,
    private val toSharedStorage: Boolean
): BaseAction(
    context,
    fileName,
    directory,
    fileExtension,
    env
) {

    private fun savePrivate(quality: Int, bitmap: Bitmap) {
        validateImageQuality(quality)
        validateWritePermission(context)
        val directory = getOrCreateDirectoryIfEmpty(directory)
        val file = File(directory, "$fileName${fileExtension.extension}")
        deleteFileIfExist(file)
        runCatching {
            val outputStream = FileOutputStream(file)
            compressBitmap(outputStream, bitmap, quality, fileExtension)
        }
    }

    private fun savePublic(quality: Int, bitmap: Bitmap) {
        validateImageQuality(quality)
        validateWritePermission(context)
        deleteExistingSharedFile(context, collection, projection, cleanDirectory, where)
        val outputStream = getOutStream(context, externalStorageDirectory, fileName, fileExtension, env)
        compressBitmap(outputStream, bitmap, quality, fileExtension)
    }

    fun save(bitmap: Bitmap, quality: Int): Boolean {
        return try {
            if(toSharedStorage){
                savePublic(quality, bitmap)
            } else {
                savePrivate(quality, bitmap)
            }
            true
        } catch (ex: Exception){
            Log.e(AndroidImageUtil.TAG, "save: ${ex.message}")
            false
        }
    }

    fun save(bitmap: Bitmap, quality: Int, imageCallBack: ImageCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if(isUsingScopeStorage){
                    savePublic(quality, bitmap)
                } else {
                    savePrivate(quality, bitmap)
                }
                imageCallBack.onSuccess()
            } catch (ex: Exception){
                Log.e(AndroidImageUtil.TAG, "save: ${ex.message}")
                imageCallBack.onFailed(ex)
            }
        }
    }

    fun getRawOutStream(outStreamCallback: OutStreamCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                validateWritePermission(context)
                if(isUsingScopeStorage){
                    deleteExistingSharedFile(context, collection, projection, cleanDirectory, where)
                } else {
                    deletePrivateFile(fileName, externalStoragePublicDir, fileExtension)
                }
                val outputStream = getOutStream(
                    context,
                    externalStorageDirectory,
                    fileName,
                    fileExtension,
                    env
                )
                withContext(Dispatchers.Main){
                    outStreamCallback.onSuccess(outputStream)
                }
            } catch (ex: Exception){
                Log.e(AndroidImageUtil.TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){ outStreamCallback.onFailed(ex) }
            }
        }
    }
}