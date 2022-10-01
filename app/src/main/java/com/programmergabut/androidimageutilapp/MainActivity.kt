package com.programmergabut.androidimageutilapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.programmergabut.androidimageutil.AndroidImageUtil.Companion.convert
import com.programmergabut.androidimageutil.AndroidImageUtil.Companion.manage
import com.programmergabut.androidimageutil.convert.Base64Callback
import com.programmergabut.androidimageutil.manage.ImageCallback
import com.programmergabut.androidimageutil.manage.LoadImageCallback
import com.programmergabut.androidimageutil.util.Extension
import com.programmergabut.androidimageutilapp.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main){

    companion object {
        const val TAKE_PHOTO_REQUEST_CODE = 1001
    }

    private val TAG = "TestMainActivity"
    private lateinit var intentSenderRequest: ActivityResultLauncher<IntentSenderRequest>

    override fun getViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding){
            btnDispatchCamera.setOnClickListener {
                dispatchTakePictureIntent()
            }
            btnDeleteImage.setOnClickListener {
                val imageDir = etImageDir.text.toString()
                val imageFile = etImageFile.text.toString()
                deletePublicImage(imageFile, imageDir)
            }
            intentSenderRequest = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        val imageDir = etImageDir.text.toString()
                        val imageFile = etImageFile.text.toString()
                        /** this line of code will arrive here if the user allow to delete file that's not this app create */
                        deletePublicImage(imageFile, imageDir)
                    }
                } else {
                    Log.d(TAG, "Failed delete public image")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            TAKE_PHOTO_REQUEST_CODE -> {
                try {
                    tryAndroidImageUtil(data)
                } catch (ex: Exception){
                    Toast.makeText(this, ex.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun tryAndroidImageUtil(data: Intent?)  {
        val captureImage = data?.extras!!["data"] as Bitmap
        val base64 = convertSection(captureImage)
        internalStorageSection(base64)
        sharedStorageSection(base64)
    }

    private fun convertSection(captureImage: Bitmap): String {
        /***
         * Example of convert to base64 with callback
         */
        convert.bitmapToBase64(captureImage, 100, Bitmap.CompressFormat.PNG,{
            Log.d(TAG, "Success convert to base64", )
        }, {
            Log.d(TAG, "Failed convert to base64")
        })

        /***
         * Example of convert to base64 without callback
         */
        return convert.bitmapToBase64(captureImage, 100, Bitmap.CompressFormat.PNG) ?: ""
    }

    private fun internalStorageSection(base64: String) {
        /***
         * Example of saving base64 to internal storage
         */
        manage(this)
            .imageAttribute("test", "folder/subfolder/", Extension.PNG)
            .save(base64, 100)

        /***
         * Example of load internal storage with callback
         */
        manage(this)
            .imageAttribute("test","folder/subfolder/", Extension.PNG)
            .load({
                Glide.with(applicationContext)
                    .load(it)
                    .into(binding.ivImage1)
                deleteImage()
            })
    }

    private fun sharedStorageSection(base64: String) {
        /***
         * Example of save public storage
         */
        manage(this)
            .imageAttribute("test_public","folder/subfolder/", Extension.PNG)
            .savePublic(base64, 100)

        /***
         * Example of load public storage with callback
         */
        manage(this)
            .imageAttribute("test_public","folder/subfolder/", Extension.PNG)
            .loadPublic({
                Log.d(TAG, "Success load image test_public")
                Glide.with(applicationContext)
                    .load(it)
                    .into(binding.ivImage2)
                deletePublicImage("test_public", "folder/subfolder/")
            },{
                Log.d(TAG, "Failed load image")
            })

        /***
         * Example getting the image URI
         */
        manage(this)
            .imageAttribute("test_public","folder/subfolder/", Extension.PNG)
            .loadPublicUri()
            .also {
                Log.d(TAG, "uri: $it")
            }
    }

    private fun deleteImage() {
        /***
         * Example of delete internal storage with callback
         */
        manage(this)
            .imageAttribute("test","folder/subfolder/", Extension.PNG)
            .delete({
                Log.d(TAG, "Success delete image test")
            },{
                Log.d(TAG, "Failed delete image")
            })
    }

    private fun deletePublicImage(imageFile: String, imageDir: String) {
        /***
         * Example of deleting public image
         */
        manage(this)
            .imageAttribute(imageFile, imageDir, Extension.JPG)
            .deletePublic(intentSenderRequest,{
                Log.d(TAG, "Success delete image $imageDir/$imageFile")
            },{
                Log.d(TAG, it.message.toString())
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            TAKE_PHOTO_REQUEST_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED
                ) {
                    dispatchTakePictureIntent()
                } else {
                    if (shouldShowReadWriteFileGranted() && shouldShowCameraPermissionRationale()){
                        Toast.makeText(this, "Please grant the permission", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Please grant the permission", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        if (!isCameraPermissionGranted() || !isReadWriteFilePermissionGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrPermissionTakePhoto, TAKE_PHOTO_REQUEST_PERMISSION_CODE)
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrPermissionTakePhoto,
                    TAKE_PHOTO_REQUEST_PERMISSION_CODE)
            }
        } else {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(this.packageManager)?.also {
                    startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST_CODE)
                }
            }
        }
    }

}