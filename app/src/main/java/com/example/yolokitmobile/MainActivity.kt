package com.example.yolokitmobile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.yolokitmobile.databinding.ActivityMainBinding
import io.github.gd182.yolokit.Detection
import io.github.gd182.yolokit.YoloKit
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private val switching = AtomicBoolean(false)

    private lateinit var models: List<SampleModel>
    @Volatile private var modelIndex = 0
    @Volatile private var kit: YoloKit? = null
    @Volatile private var loadError: String? = null
    private var spinnerUserAction = false
    private var lastFpsTs = 0L
    private var frames = 0

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (loaded, defaultId) = ModelRegistry.load(applicationContext)
        models = loaded
        modelIndex = models.indexOfFirst { it.id == defaultId }.coerceAtLeast(0)

        setupModelSpinner()
        loadModel(modelIndex)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupModelSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            models.map { it.displayName },
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.modelSpinner.adapter = adapter
        binding.modelSpinner.setSelection(modelIndex, false)
        // Only a real touch on the spinner counts as a user choice; the automatic callback
        // fired when the listener attaches must not trigger a model reload.
        binding.modelSpinner.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) spinnerUserAction = true
            false
        }
        binding.modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!spinnerUserAction) return
                spinnerUserAction = false
                if (position != modelIndex) loadModel(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinnerUserAction = false
            }
        }
    }

    private fun loadModel(index: Int) {
        if (!switching.compareAndSet(false, true)) {
            binding.modelSpinner.setSelection(modelIndex, false)
            return
        }
        modelIndex = index
        val model = models[index]
        binding.statusText.text = "loading ${model.displayName}…"
        analysisExecutor.execute {
            kit?.close()
            kit = null
            val result = YoloKit.create(applicationContext, model.config)
            kit = result.getOrNull()
            loadError = result.exceptionOrNull()?.let { "load failed: ${model.displayName}\n${it.message}" }
            loadError?.let { Log.e("MainActivity", it) }
            switching.set(false)
            runOnUiThread {
                binding.overlay.setResults(emptyList(), 1, 1)
                binding.statusText.text = kit?.let { "${model.displayName}\n${it.backendLabel}" } ?: loadError
            }
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    )
                )
                .build()

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(analysisExecutor, ::analyze) }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(image: ImageProxy) {
        val current = kit
        if (current == null || switching.get() || !busy.compareAndSet(false, true)) {
            image.close()
            return
        }
        try {
            val bitmap = image.toUprightBitmap()
            val started = System.nanoTime()
            val detections = current.detect(bitmap)
            val ms = (System.nanoTime() - started) / 1_000_000.0

            runOnUiThread {
                binding.overlay.setResults(detections, bitmap.width, bitmap.height)
                updateFps(ms)
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "analyze failed", t)
        } finally {
            busy.set(false)
            image.close()
        }
    }

    private fun updateFps(inferMs: Double) {
        frames++
        val now = System.currentTimeMillis()
        if (lastFpsTs == 0L) lastFpsTs = now
        if (now - lastFpsTs >= 1000) {
            val fps = frames * 1000f / (now - lastFpsTs)
            val name = models[modelIndex].displayName
            val backend = kit?.backendLabel ?: "-"
            binding.statusText.text = "%s\n%s · %.1f FPS · %.0f ms".format(name, backend, fps, inferMs)
            Log.i("MainActivity", "%s | %s | %.1f FPS | %.0f ms".format(name, backend, fps, inferMs))
            frames = 0
            lastFpsTs = now
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.execute {
            kit?.close()
            kit = null
        }
        analysisExecutor.shutdown()
    }
}

/** Convert an RGBA_8888 [ImageProxy] to an upright ARGB_8888 [Bitmap]. */
private fun ImageProxy.toUprightBitmap(): Bitmap {
    val plane = planes[0]
    val bmp = Bitmap.createBitmap(
        plane.rowStride / plane.pixelStride,
        height,
        Bitmap.Config.ARGB_8888,
    )
    bmp.copyPixelsFromBuffer(plane.buffer)
    val cropped = if (bmp.width != width) {
        Bitmap.createBitmap(bmp, 0, 0, width, height)
    } else {
        bmp
    }

    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return cropped
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
}
