# Models in assets

**English** · [Русский](README.ru.md)

[The app](../../../../README.md) reads its model configuration, labels, and
model files from this directory. Models are packaged into the APK at build time.
The installed app does not download models or import external files.

Git tracks [models.example.json](models.example.json) and [labels.txt](labels.txt).
Your local `models.json`, `*.param`, and `*.bin` files are ignored by Git,
but are still included in the APK. Put the weights here before building.

Run all commands below from the YoloKitMobile repository root.

## Configuration

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

If `models.json` is absent, the app uses the example file. Its filenames and
classes are placeholders: without the corresponding models, detection reports
`load failed`.

Each model needs an `id`, its model files, and `labels`. Set `default` to the
ID of the model to load first. Labels can be an array of class names or an asset
filename containing one class per line:

```json
{
  "labels": "labels.txt"
}
```

This is a field example, not a complete model entry. The bundled `labels.txt`
contains COCO labels. Use it only with a model trained on those same 80 classes.
For a custom model, supply its class names in training order; do not sort them
alphabetically.

See the [app README](../../../../README.md#configuration-fields) for all fields
and their defaults.

## Export an ncnn model

For an Ultralytics object detector with a compatible output:

```bash
python -m pip install ultralytics
yolo export model=best.pt format=ncnn imgsz=640
cp best_ncnn_model/model.ncnn.param best_ncnn_model/model.ncnn.bin app/src/main/assets/
```

Export options are documented in
[Ultralytics' ncnn guide](https://docs.ultralytics.com/integrations/ncnn/).
Supported outputs are listed in
[YoloKit's model requirements](../../../../vendor/YoloKit/README.md#model-requirements).
An ncnn export is not automatically compatible: the API handles object detection,
not segmentation, classification, or pose estimation.

A complete `models.json` for a model with two classes:

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

Replace `labels` with your model's classes. Check their order against
`best_ncnn_model/metadata.yaml`. Set `targetSize` to the export's `imgsz`
and explicitly use `bgr: false` for RGB input. A mismatch in input size,
color order, or tensor names can produce incorrect results.

A raw YOLOv8 DFL export usually needs `decoded: false`,
`inputName: "images"`, `outputName: "output"`, and `bgr: true`.
It has a different output format; choose settings based on the model graph,
not the file extension.

### Load from bytes

Add `"loadAsBytes": true` to an ncnn entry to read the same assets into arrays
and pass them to YoloKit. This demonstrates loading from memory. It does not
encrypt the files or move them outside the APK.

This field is not used for QNN entries.

## QNN

Build a QNN context file for the phone's HTP architecture and runtime version.
See [QNN.md](../../../../docs/QNN.md) or
[QNN.ru.md](../../../../docs/QNN.ru.md). Both conversion scripts save their
output here by default:

- [macOS / connected phone](../../../../scripts/build_qnn_model.sh)
- [Ubuntu x86_64](../../../../scripts/build_qnn_model_ubuntu_x86.sh)

A configuration for `model_v81.bin` with two classes:

```json
{
  "default": "my-qnn-model",
  "models": [
    {
      "id": "my-qnn-model",
      "displayName": "My YOLO model (QNN)",
      "backend": "qnn",
      "model": "model_v81.bin",
      "boxesNormalized": false,
      "confThreshold": 0.25,
      "nmsThreshold": 0.45,
      "labels": ["person", "car"]
    }
  ]
}
```

Replace the class names and filename with your own. Use `boxesNormalized: true`
only for coordinates in the 0–1 range. QNN reads the input size and type from
the context file; you do not need to set model precision manually.

A QNN model appearing in the dropdown does not confirm context-file
compatibility. The app does not automatically select a model by SoC.
QNN entries are hidden on devices that fail the availability check, so include
an ncnn model as well if the APK is intended for different phones.

## Build and check

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s MainActivity YoloNcnn QnnYolo
```

For `load failed`, check filenames and whether the model files are in the APK.
For shifted boxes, check the export format and input size. For wrong classes,
check label count and order. Before releasing an APK, check which local models
and configuration were packaged into its assets.

License terms are in [LICENSE](../../../../LICENSE) and
[NOTICE](../../../../NOTICE). The source code's license does not determine
the license of the weights you add.
