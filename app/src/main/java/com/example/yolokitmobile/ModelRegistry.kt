package com.example.yolokitmobile

import android.content.Context
import io.github.gd182.yolokit.Backend
import io.github.gd182.yolokit.ModelConfig
import io.github.gd182.yolokit.YoloKit
import org.json.JSONArray
import org.json.JSONObject

/** One entry of `assets/models.json`, turned into a library config. */
data class SampleModel(
    val id: String,
    val displayName: String,
    val backend: Backend,
    val config: ModelConfig,
)

/**
 * Reads `assets/models.json` (falls back to `models.example.json`).
 * The library itself knows nothing about JSON: this is only the sample's way to list models.
 */
object ModelRegistry {
    private const val LOCAL = "models.json"
    private const val EXAMPLE = "models.example.json"

    fun load(context: Context): Pair<List<SampleModel>, String> {
        val text = runCatching { context.assets.open(LOCAL).bufferedReader().use { it.readText() } }
            .getOrElse { context.assets.open(EXAMPLE).bufferedReader().use { it.readText() } }
        val root = JSONObject(text)
        val arr = root.getJSONArray("models")
        val models = (0 until arr.length()).map { parse(context, arr.getJSONObject(it)) }
            .filter { it.backend == Backend.NCNN || YoloKit.isQnnAvailable(context) }
        require(models.isNotEmpty()) { "no models in $LOCAL are usable on this device" }
        val default = root.optString("default").ifEmpty { models.first().id }
        return models to default
    }

    private fun parse(context: Context, o: JSONObject): SampleModel {
        val id = o.getString("id")
        val name = o.optString("displayName", id)
        val labels = readLabels(context, o.get("labels"))
        val builder = ModelConfig.Builder()
            .labels(labels)
            .inputSize(o.optInt("targetSize", 640))
            .thresholds(
                o.optDouble("confThreshold", 0.25).toFloat(),
                o.optDouble("nmsThreshold", 0.45).toFloat(),
            )
        val backend = when (o.optString("backend", "ncnn")) {
            "qnn" -> {
                builder.qnn(o.getString("model"), o.optBoolean("boxesNormalized", false))
                Backend.QNN
            }
            else -> {
                builder.ncnn(
                    param = o.getString("param"),
                    bin = o.getString("bin"),
                    decoded = o.optBoolean("decoded", false),
                    inputName = o.optString("inputName", if (o.optBoolean("decoded")) "in0" else "images"),
                    outputName = o.optString("outputName", if (o.optBoolean("decoded")) "out0" else "output"),
                ).rgbOrder(!o.optBoolean("bgr", true))
                Backend.NCNN
            }
        }
        return SampleModel(id, name, backend, builder.build())
    }

    private fun readLabels(context: Context, node: Any): List<String> = when (node) {
        is JSONArray -> (0 until node.length()).map { node.getString(it) }
        is String -> context.assets.open(node).bufferedReader().use { r ->
            r.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        }
        else -> error("models.json: 'labels' must be an array or an asset filename")
    }
}
