package com.example.myapplication.ui.camera.filters

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.google.common.util.concurrent.ListenableFuture

class DeepARManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val surfaceView: SurfaceView
) : DefaultLifecycleObserver, SurfaceHolder.Callback, AREventListener {
    
    private val TAG = "DeepARManager"
    
    // DeepAR instance
    private var deepAR: DeepAR? = null
    
    // Currently active filter
    private var currentFilter: DeepARFilter? = null
    
    // Camera state
    private var cameraStarted = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    
    // Camera processing
    private val NUMBER_OF_BUFFERS = 2
    private var buffers: Array<ByteBuffer>? = null
    private var currentBuffer = 0
    
    // Available filters from DeepAR example
    private val availableFilters = listOf(
        DeepARFilter("None", null),
        DeepARFilter("Viking Helmet", "Viking Helmet PBR/viking_helmet.deepar"),
        DeepARFilter("Makeup Look", "Makeup Look Simple/MakeupLook.deepar"),
        DeepARFilter("Split View", "Makeup Look w: Slipt Screen Effect/Split_View_Look.deepar"),
        DeepARFilter("Emotions", "Emotions Exaggerator/Emotions_Exaggerator.deepar"),
        DeepARFilter("Emotion Meter", "Emotion Meter/Emotion_Meter.deepar"),
        DeepARFilter("Stallone", "Stallone/Stallone.deepar"),
        DeepARFilter("Flower Face", "Flower Face/flower_face.deepar"),
        DeepARFilter("Humanoid", "Humanoid/Humanoid.deepar"),
        DeepARFilter("Devil Horns", "Devil Neon Horns/Neon_Devil_Horns.deepar"),
        DeepARFilter("Ping Pong", "Ping Pong Minigame/Ping_Pong.deepar"),
        DeepARFilter("Pixel Hearts", "Pixel Heart Particles/8bitHearts.deepar"),
        DeepARFilter("Snail", "Snail/Snail.deepar"),
        DeepARFilter("Hope", "Hope/Hope.deepar"),
        DeepARFilter("Vendetta Mask", "Vendetta Mask/Vendetta_Mask.deepar"),
        DeepARFilter("Fire Effect", "Fire Effect/Fire_Effect.deepar"),
        DeepARFilter("Elephant Trunk", "Elephant Trunk/Elephant_Trunk.deepar")
    )
    
    // Bitmap for the latest screenshot
    private var screenshotBitmap: Bitmap? = null
    private var screenshotUri: Uri? = null
    
    init {
        Log.d(TAG, "Initializing DeepAR Manager")
        surfaceView.holder.addCallback(this)
        initializeDeepAR()
    }
    
    private fun initializeDeepAR() {
        try {
            Log.d(TAG, "Creating DeepAR instance")
            deepAR = DeepAR(context)
            deepAR?.let { ar ->
                ar.setLicenseKey("4f246fb03377629e1140c42d360d4f476b670ade4296b1ce182831ee0717a5fb4d07657203bb41e5")
                ar.initialize(context, this)
            }
            Log.d(TAG, "DeepAR instance created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing DeepAR", e)
        }
    }
    
    private fun setupCamera(lifecycleOwner: LifecycleOwner) {
        try {
            cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture?.addListener({
                try {
                    val provider = cameraProviderFuture?.get()
                    if (provider != null) {
                        cameraProvider = provider
                        bindImageAnalysis(lifecycleOwner)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting camera provider", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera", e)
        }
    }

    private fun bindImageAnalysis(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return
        
        try {
            // Unbind all use cases
            cameraProvider.unbindAll()
            
            // Create buffers for image processing
            if (buffers == null) {
                buffers = Array(NUMBER_OF_BUFFERS) {
                    ByteBuffer.allocateDirect(1080 * 1920 * 4).apply {
                        order(ByteOrder.nativeOrder())
                        position(0)
                    }
                }
            }
            
            // Configure camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            // Configure image analysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                        processImage(image)
                    }
                }

            // Bind use cases
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            
            cameraStarted = true
            Log.d(TAG, "Camera setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera use cases", e)
        }
    }

    private fun processImage(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            buffer.rewind()
            
            buffers?.get(currentBuffer)?.let { currentBuf ->
                currentBuf.put(buffer)
                currentBuf.position(0)
                
                deepAR?.receiveFrame(
                    currentBuf,
                    image.width,
                    image.height,
                    image.imageInfo.rotationDegrees,
                    true, // Mirror for front camera
                    DeepARImageFormat.RGBA_8888,
                    image.planes[0].pixelStride
                )
            }
            
            currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS
        } catch (e: Exception) {
            Log.e(TAG, "Error processing camera frame", e)
        } finally {
            image.close()
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
        Log.d(TAG, "Applying filter: ${filter.name}, path: ${filter.filePath}")
        currentFilter = filter
        try {
            if (filter.filePath == null) {
                deepAR?.switchEffect("effect", null as String?)
                Log.d(TAG, "Cleared filter")
            } else {
                val path = getFilterPath(filter.filePath)
                Log.d(TAG, "Full filter path: $path")
                deepAR?.switchEffect("effect", path)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying filter", e)
        }
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
        Log.d(TAG, "Taking screenshot")
        deepAR?.takeScreenshot()
    }
    
    /**
     * Returns the URI of the last screenshot taken
     */
    fun getLastScreenshotUri(): Uri? = screenshotUri
    
    /**
     * Start the camera feed
     */
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        if (cameraStarted) {
            Log.d(TAG, "Camera already started")
            return
        }
        
        try {
            Log.d(TAG, "Starting camera")
            setupCamera(lifecycleOwner)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
        }
    }
    
    /**
     * Stop the camera feed
     */
    fun stopCamera() {
        if (!cameraStarted) {
            return
        }
        
        try {
            Log.d(TAG, "Stopping camera")
            cameraProvider?.unbindAll()
            cameraStarted = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: width=$width, height=$height")
        // Set the surface where DeepAR will render
        deepAR?.setRenderSurface(holder.surface, width, height)
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        stopCamera()
        deepAR?.setRenderSurface(null, 0, 0)
    }
    
    override fun screenshotTaken(bitmap: Bitmap) {
        Log.d(TAG, "Screenshot taken")
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
            Log.d(TAG, "Screenshot saved to: ${screenshotUri?.path}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving screenshot", e)
        }
    }
    
    override fun videoRecordingStarted() {
        Log.d(TAG, "Video recording started")
    }
    
    override fun videoRecordingFinished() {
        Log.d(TAG, "Video recording finished")
    }
    
    override fun videoRecordingFailed() {
        Log.e(TAG, "Video recording failed")
    }
    
    override fun videoRecordingPrepared() {
        Log.d(TAG, "Video recording prepared")
    }
    
    override fun shutdownFinished() {
        Log.d(TAG, "Shutdown finished")
    }
    
    override fun initialized() {
        Log.d(TAG, "DeepAR initialized successfully")
        // Start the camera automatically when DeepAR is initialized
        startCamera(context as LifecycleOwner)
        
        // Apply initial filter if needed
        currentFilter?.let { applyFilter(it) }
    }
    
    override fun faceVisibilityChanged(visible: Boolean) {
        Log.d(TAG, "Face visibility changed: $visible")
    }
    
    override fun imageVisibilityChanged(image: String, visible: Boolean) {
        Log.d(TAG, "Image visibility changed: $image, visible: $visible")
    }
    
    override fun frameAvailable(image: Image) {
        // Frame available, not logging to avoid spam
    }
    
    override fun error(arErrorType: ARErrorType, message: String) {
        Log.e(TAG, "DeepAR error: $arErrorType - $message")
    }
    
    override fun effectSwitched(effect: String) {
        Log.d(TAG, "Effect switched: $effect")
    }
    
    override fun onResume(owner: LifecycleOwner) {
        Log.d(TAG, "onResume")
        if (!cameraStarted) {
            startCamera(owner)
        }
        super.onResume(owner)
    }
    
    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "onPause")
        stopCamera()
        super.onPause(owner)
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(TAG, "onDestroy")
        stopCamera()
        deepAR?.release()
        deepAR = null
        super.onDestroy(owner)
    }
}