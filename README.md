<h1 align="center">YoloKitMobile</h1>

<p align="center">A small Android sample that runs YOLO object detection on the camera preview.</p>

<div align="center">
  <img src="https://skillicons.dev/icons?i=kotlin" height="40" alt="Kotlin logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=cpp" height="40" alt="C++ logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=cmake" height="40" alt="CMake logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=gradle" height="40" alt="Gradle logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=androidstudio" height="40" alt="Android Studio logo" />
</div>

<p align="center"><b>English</b> · <a href="README.ru.md">Русский</a></p>

YoloKitMobile is a camera app for trying YOLO models on Android. It uses
[CameraX](https://developer.android.com/media/camera/camerax) for the camera and
[YoloKit](https://github.com/gd182/YoloKit) for detection, then draws class names,
confidence scores, and boxes over the preview.

ncnn runs on Vulkan when available and falls back to CPU. Compatible Snapdragon
devices can also run a QNN context binary on the Hexagon NPU. The app lets you
switch models, stop and restart detection, and view the backend, model precision,
FPS, and time spent in `detect()`.

## Install or build

Application releases contain version notes and source code, not a ready-to-use
APK with YOLO model weights, because of the licensing terms of the models used
by this project. See [Releases](https://github.com/gd182/YoloKitMobile/releases)
for the version history. Build the app locally with your own model files.
Weights are not stored in this repository, and the app does not download or
import models at runtime.

For a local build, clone the app with its library submodule:

```bash
git clone --recurse-submodules https://github.com/gd182/YoloKitMobile.git
cd YoloKitMobile
```

If you already cloned it, run:

```bash
git submodule update --init --recursive
```

Open the project in Android Studio with support for AGP 9.4.0. Install Android
SDK 37, NDK 28.2.13676358, and CMake 3.22.1. Use the included Gradle wrapper
(9.6.0). The app's Gradle daemon criteria request JDK 25; Gradle can provision
that toolchain. Java source and target compatibility remain 17.
See [daemon settings](gradle/gradle-daemon-jvm.properties) and
[app build settings](app/build.gradle.kts) for the exact configuration.

The app supports Android 7.0 and newer and builds for `arm64-v8a`,
`armeabi-v7a`, and `x86_64`. QNN is arm64-only.

### Library revision

The app uses `includeBuild("vendor/YoloKit")`, so it builds against the
checked-in submodule, not a sibling clone or a downloaded AAR.
[The local library README](vendor/YoloKit/README.md) describes that revision;
[the YoloKit repository](https://github.com/gd182/YoloKit) contains newer work.

### Build commands

Add your model files and configuration as described below, then build and install:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For an explicit ncnn-only native build and lint check:

```bash
./gradlew -Pqnn.sdk.dir= :app:assembleDebug :app:lintDebug
```

The [CI workflow](.github/workflows/android.yml) runs this ncnn-only build and
uploads a debug APK artifact. It does not publish a release or validate QNN on
a phone. A release APK needs signing; see
[Android app signing](https://developer.android.com/studio/publish/app-signing).

## Add models to the APK

All models are loaded from `app/src/main/assets/`. Copy the configuration
template and replace the placeholder filenames and labels:

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

For example, an Ultralytics ncnn detector with two classes:

```json
{
  "default": "my-model",
  "models": [
    {
      "id": "my-model",
      "displayName": "My YOLO model",
      "backend": "ncnn",
      "param": "model.ncnn.param",
      "bin": "model.ncnn.bin",
      "inputName": "in0",
      "outputName": "out0",
      "targetSize": 640,
      "decoded": true,
      "bgr": false,
      "confThreshold": 0.25,
      "nmsThreshold": 0.45,
      "labels": ["person", "car"]
    }
  ]
}
```

Place `model.ncnn.param` and `model.ncnn.bin` beside `models.json`.
Labels must match your model, not this example. The `default` field is a model
ID; if it is absent or not in the usable list, the app selects the first model.

`models.json` and weight files are ignored by Git but still included in the
APK when present at build time. If the local registry is absent, the app reads
[models.example.json](app/src/main/assets/models.example.json). That file is a
template, not a bundled working model.

### Configuration fields

| Field | Meaning and default |
|---|---|
| `id` | Model identifier used by `default` |
| `displayName` | Dropdown label; defaults to `id` |
| `backend` | `ncnn` (default) or `qnn` |
| `param` / `bin` | Required ncnn asset filenames |
| `model` | Required QNN context binary asset filename |
| `targetSize` | ncnn export input size, a positive multiple of 32; defaults to 640 |
| `decoded` | ncnn box decoding in the graph; defaults to `false` |
| `inputName` / `outputName` | `images` / `output`, or `in0` / `out0` with `decoded: true` |
| `bgr` | ncnn pixel order; defaults to `true`, so set `false` for an RGB export |
| `confThreshold` / `nmsThreshold` | Defaults to 0.25 / 0.45 |
| `labels` | Required class-name array or asset filename, one class per line |
| `boxesNormalized` | QNN boxes in the 0–1 range; defaults to `false` |
| `loadAsBytes` | ncnn assets read into `ByteArray`; defaults to `false` |

`targetSize` must match the ncnn export's `imgsz`. QNN reads its actual input
shape from the context binary. For Ultralytics ncnn exports, explicitly use
`decoded: true`, `bgr: false`, `inputName: "in0"`, and `outputName: "out0"`.

`loadAsBytes` demonstrates memory-based loading, such as the path an app
would use after decryption. It does not encrypt models, load external files,
or apply to QNN entries in this sample.

[The assets README](app/src/main/assets/README.md) contains export commands and
full ncnn and QNN examples. Supported outputs are listed in
[YoloKit's model requirements](vendor/YoloKit/README.md#model-requirements).

## Use the camera

Grant camera permission and choose a model from the dropdown. The app uses
the rear camera. The overlay shows the class name and confidence for each box.

**Stop** unloads the detector and, for ncnn, requests release of Vulkan resources.
The camera preview stays active. **Start** reloads the same model, so resuming
can take time. Selecting another model closes the previous detector and loads
the new one.

The status shows the active backend and detected precision. FPS counts processed
frames; the displayed time measures `detect()`, not the full camera pipeline.

## QNN on Snapdragon

QNN needs both native SDK support and a context binary compatible with the
phone's HTP architecture. Configure QAIRT 2.50.0 in
`vendor/YoloKit/local.properties`:

```properties
qnn.sdk.dir=/path/to/QAIRT/2.50.0
```

You can also set `QNN_SDK_ROOT` or pass `-Pqnn.sdk.dir=/path/to/QAIRT/2.50.0`.
The directory must contain `include/QNN/QnnInterface.h`. SDK headers,
conversion tools, and the library's Maven runtime must use matching versions.

Add an entry with `backend: "qnn"`, `model`, and the correct `labels`.
QNN entries are shown only when YoloKit's device/build availability check passes.
This is not model compatibility detection: a wrong context binary can still
fail to load, and models are not selected by SoC automatically.

Conversion and device setup:
[QNN guide](docs/QNN.md) /
[Russian guide](docs/QNN.ru.md).

Scripts:
[macOS / connected phone](scripts/build_qnn_model.sh) /
[Ubuntu x86_64](scripts/build_qnn_model_ubuntu_x86.sh).
Run each script with `--help` for its options.

## Troubleshooting

| Symptom | What to check |
|---|---|
| `load failed` | Model files included in APK assets, filenames, labels, and export settings |
| QNN missing from the dropdown | arm64 Qualcomm device and QNN compiled into the library |
| QNN model fails after appearing in the list | Runtime libraries, HTP architecture, and SDK version of the binary |
| Shifted boxes or incorrect classes | Input size, export format, color order, and label order |
| `restart failed` | Loading error in the status line; the detector remains stopped |

Logs are available with:

```bash
adb logcat -s MainActivity YoloNcnn QnnYolo
```

## Code and related projects

[MainActivity.kt](app/src/main/java/com/example/yolokitmobile/MainActivity.kt)
handles CameraX, model switching, and the processing thread.
[ModelRegistry.kt](app/src/main/java/com/example/yolokitmobile/ModelRegistry.kt)
turns the JSON into YoloKit configuration.
[OverlayView.kt](app/src/main/java/com/example/yolokitmobile/OverlayView.kt)
maps detections onto the cropped preview.

Camera frames are converted to upright ARGB_8888 bitmaps. YoloKit handles
padding, inference, decoding, and NMS without OpenCV. Its API and AAR integration
are documented in [YoloKit](https://github.com/gd182/YoloKit) and
[the AAR guide](https://github.com/gd182/YoloKit/blob/main/docs/AAR.md).
Library releases will include an AAR without model weights; see
[YoloKit Releases](https://github.com/gd182/YoloKit/releases).

## License

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for project and
third-party terms. Model weights have their own licenses.
