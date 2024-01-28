package com.ren.permissionsexample

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.ren.permissionsexample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onFabGalleryClick()
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            kotlin.runCatching {
                uri?.let {
                    binding.ivImage.setImageURI(it)
                }
            }.onFailure { t ->
                showSnackbar(t.message ?: "Unknown error!")
            }
        }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            kotlin.runCatching {
                uri?.let {
                    binding.ivImage.setImageURI(it)
                }
            }.onFailure { t ->
                showSnackbar(t.message ?: "Unknown error!")
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            when {
                isGranted -> {
                    galleryLauncher.launch("image/*")
                }

                !ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    showFeatureIsUnavailableAlertDialog()
                }

                else -> {
                    requestPermission()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readMediaImagesIsGranted =
                permissions[android.Manifest.permission.READ_MEDIA_IMAGES]
            val readMediaVideoIsGranted = permissions[android.Manifest.permission.READ_MEDIA_VIDEO]

            when {
                readMediaImagesIsGranted == true && readMediaVideoIsGranted == true -> {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }

                !ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission_group.READ_MEDIA_VISUAL
                ) -> {
                    showFeatureIsUnavailableAlertDialog()
                }

                else -> {
                    requestPermission()
                }
            }
        }

    private fun onFabGalleryClick() {
        binding.fabGallery.setOnClickListener {
            requestPermission()
        }
    }

    private fun requestPermission() {
        if (VERSION.SDK_INT <= 32) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    galleryLauncher.launch("image/*")
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    showPermissionRequiredAlertDialog()
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) -> {
                    showPermissionRequiredAlertDialog()
                }

                else -> {
                    requestMultiplePermissions.launch(
                        arrayOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO
                        )
                    )
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.ok)) {
                dismiss()
            }
            show()
        }
    }

    private fun showPermissionRequiredAlertDialog() {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
        builder.apply {
            setTitle(getString(R.string.permission_required))
            setMessage(getString(R.string.permission_required_message))
            setCancelable(false)
            setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                if (VERSION.SDK_INT <= 32) {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    requestMultiplePermissions.launch(
                        arrayOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO
                        )
                    )
                }

                dialog.dismiss()
            }
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun showFeatureIsUnavailableAlertDialog() {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
        builder.apply {
            setTitle(getString(R.string.permission_required))
            setMessage(getString(R.string.feature_is_unavailable))
            setCancelable(false)
            setPositiveButton(getString(R.string.setting)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)

                dialog.dismiss()
            }
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }
}