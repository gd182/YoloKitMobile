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

YoloKitMobile runs YOLO on the Android camera feed and draws boxes around detected
objects. It is a sample app for [YoloKit](https://github.com/gd182/YoloKit),
with CameraX for the camera and a model selector for switching between models.

Detection runs through ncnn on Vulkan, with CPU fallback. Supported Snapdragon
devices can also use Qualcomm QNN on the Hexagon NPU. The app shows the active
backend, model precision, FPS, and inference time.

## Build and run

Clone the app with its YoloKit submodule:

```bash
git clone --recurse-submodules https://github.com/gd182/YoloKitMobile.git
cd YoloKitMobile
```

For an existing clone, run `git submodule update --init --recursive`.

Open the project in Android Studio. You will need JDK 17, Android SDK 37,
NDK 28.2.13676358, and CMake 3.22.1. The project uses AGP 9.4.0 and supports
Android 7.0 and newer. It builds for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

To build and install from the terminal:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Model weights are not included. Without them the app builds, but detection
reports `load failed` until you add a model.

## Add a model

Copy the example configuration:

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

Put your model files in `app/src/main/assets/` and edit `models.json` to match.
Both the configuration and weights are ignored by Git. If `models.json` is
missing, the app reads `models.example.json`.

Each model has an `id`, a `displayName`, and a `backend` (`ncnn` or `qnn`).
Set `default` to the ID of the model you want to load first. You can switch
models from the dropdown in the app.

| Setting | Value |
|---|---|
| `param` / `bin` | ncnn filenames in assets |
| `model` | QNN context binary filename in assets |
| `inputName` / `outputName` | ncnn tensor names |
| `targetSize` | Export input size (`imgsz`); defaults to 640 |
| `decoded` | Whether the ncnn graph decodes boxes |
| `bgr` | Whether the ncnn input uses BGR rather than RGB |
| `confThreshold` / `nmsThreshold` | Confidence and NMS thresholds |
| `labels` | Class names in training order, as an array or an asset filename |
| `boxesNormalized` | Whether QNN box coordinates are normalized |

For Ultralytics `format=ncnn`, use `decoded: true`, `bgr: false`,
`inputName: "in0"`, and `outputName: "out0"`. Set `targetSize` to the
`imgsz` used during export. A mismatch can cause shifted or incorrectly sized boxes.

See the [assets README](app/src/main/assets/README.md) for export commands
and [YoloKit](vendor/YoloKit/README.md#model-requirements) for supported model outputs.

## QNN

QNN needs a context binary built for the target Snapdragon device and is
available only on `arm64-v8a`. Models using QNN appear in the dropdown only
when the backend is available on the phone.

To compile QNN support, set `qnn.sdk.dir` in `vendor/YoloKit/local.properties`,
or set `QNN_SDK_ROOT` to your QAIRT SDK directory. The SDK must match the QNN
runtime version used by YoloKit. Without the SDK, the app builds with ncnn only.

The conversion and device setup are covered in [QNN.md](docs/QNN.md).

## Source

`app/` contains the camera UI, model registry, and overlay.
`vendor/YoloKit/` is the detector library, connected through a Gradle composite
build. Model conversion scripts are in `scripts/`; QNN documentation is in `docs/`.

CameraX frames are converted to upright ARGB_8888 bitmaps before detection.
YoloKit handles image padding, inference, box decoding, and NMS without OpenCV.

## License

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for project and
third-party terms. Model weights have their own licenses.
