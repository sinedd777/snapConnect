package com.example.myapplication.ui.camera.filters

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ai.deepar.ar.AREventListener
import ai.deepar.ar.ARErrorType
import ai.deepar.ar.DeepAR
import ai.deepar.ar.DeepARImageFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.Image

class DeepARManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val surfaceView: SurfaceView
) : DefaultLifecycleObserver, SurfaceHolder.Callback, AREventListener {
    
    // DeepAR instance
    private var deepAR: DeepAR? = null
    
    // Currently active filter
    private var currentFilter: DeepARFilter? = null
    
    // Available filters from DeepAR example
    private val availableFilters = listOf(
        DeepARFilter("None", null),
        DeepARFilter("Viking Helmet", "viking_helmet.deepar"),
        DeepARFilter("Makeup Look", "MakeupLook.deepar"),
        DeepARFilter("Split View", "Split_View_Look.deepar"),
        DeepARFilter("Emotions", "Emotions_Exaggerator.deepar"),
        DeepARFilter("Emotion Meter", "Emotion_Meter.deepar"),
        DeepARFilter("Stallone", "Stallone.deepar"),
        DeepARFilter("Flower Face", "flower_face.deepar"),
        DeepARFilter("Humanoid", "Humanoid.deepar"),
        DeepARFilter("Devil Horns", "Neon_Devil_Horns.deepar"),
        DeepARFilter("Ping Pong", "Ping_Pong.deepar"),
        DeepARFilter("Pixel Hearts", "Pixel_Hearts.deepar"),
        DeepARFilter("Snail", "Snail.deepar"),
        DeepARFilter("Hope", "Hope.deepar"),
        DeepARFilter("Vendetta Mask", "Vendetta_Mask.deepar"),
        DeepARFilter("Fire Effect", "Fire_Effect.deepar"),
        DeepARFilter("Elephant Trunk", "Elephant_Trunk.deepar")
    )
    
    // Bitmap for the latest screenshot
    private var screenshotBitmap: Bitmap? = null
    private var screenshotUri: Uri? = null
    
    init {
        surfaceView.holder.addCallback(this)
        initializeDeepAR()
    }
    
    private fun initializeDeepAR() {
        deepAR = DeepAR(context).apply {
            setLicenseKey("4f246fb03377629e1140c42d360d4f476b670ade4296b1ce182831ee0717a5fb4d07657203bb41e5")
            initialize(context, this@DeepARManager)
        }
    }
    
    /**
     * Returns the list of available AR filters
     */
    fun getAvailableFilters(): List<DeepARFilter> = availableFilters
    
    /**
     * Applies the selected filter
     */
    fun applyFilter(filter: DeepARFilter) {
        currentFilter = filter
        deepAR?.switchEffect("effect", getFilterPath(filter.filePath))
    }
    
    private fun getFilterPath(filterName: String?): String? {
        if (filterName == null) {
            return null
        }
        return "file:///android_asset/$filterName"
    }
    
    /**
     * Takes a screenshot using DeepAR
     */
    fun takeScreenshot() {
        deepAR?.takeScreenshot()
    }
    
    /**
     * Returns the URI of the last screenshot taken
     */
    fun getLastScreenshotUri(): Uri? = screenshotUri
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface is created
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Set the surface where DeepAR will render
        deepAR?.setRenderSurface(holder.surface, width, height)
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        deepAR?.setRenderSurface(null, 0, 0)
    }
    
    override fun screenshotTaken(bitmap: Bitmap) {
        screenshotBitmap = bitmap
        
        // Save the bitmap to a file
        val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
        val now = dateFormat.format(Date())
        val imageFile = File(context.cacheDir, "deepar_screenshot_$now.jpg")
        
        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            screenshotUri = Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Other AREventListener methods
    override fun videoRecordingStarted() {}
    override fun videoRecordingFinished() {}
    override fun videoRecordingFailed() {}
    override fun videoRecordingPrepared() {}
    override fun shutdownFinished() {}
    override fun initialized() {
        // Apply initial filter if needed
        currentFilter?.let { applyFilter(it) }
    }
    override fun faceVisibilityChanged(visible: Boolean) {}
    override fun imageVisibilityChanged(image: String, visible: Boolean) {}
    override fun frameAvailable(image: Image) {}
    override fun error(arErrorType: ARErrorType, message: String) {}
    override fun effectSwitched(effect: String) {}
    
    override fun onDestroy(owner: LifecycleOwner) {
        deepAR?.release()
        deepAR = null
        super.onDestroy(owner)
    }
}