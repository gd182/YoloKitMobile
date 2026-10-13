<h1 align="center">YoloKitMobile</h1>

<p align="center">Небольшое Android-приложение для распознавания объектов в кадре камеры с помощью YOLO.</p>

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

<p align="center"><a href="README.md">English</a> · <b>Русский</b></p>

YoloKitMobile — приложение для проверки моделей YOLO на камере Android.
Камера работает через [CameraX](https://developer.android.com/media/camera/camerax),
а распознавание — через [YoloKit](https://github.com/gd182/YoloKit).
Поверх изображения отображаются рамки, названия классов и уверенность модели.

ncnn использует Vulkan, если он доступен, или выполняет вычисления на CPU.
На совместимых устройствах Snapdragon можно запускать QNN-модель на NPU Hexagon.
В приложении можно переключать модели, останавливать и запускать распознавание,
смотреть активный бэкенд, разрядность модели, FPS и время вызова `detect()`.

## Установка и сборка

В релизах приложения публикуются описания версий и исходный код.
Готовый APK с весами YOLO не прикладывается из-за условий лицензий моделей,
используемых в проекте. История версий находится в
[Releases](https://github.com/gd182/YoloKitMobile/releases).
Соберите приложение локально со своими файлами моделей.
Весов в репозитории нет; приложение не скачивает и не импортирует их
во время работы.

Для своей сборки клонируйте проект вместе с библиотекой:

```bash
git clone --recurse-submodules https://github.com/gd182/YoloKitMobile.git
cd YoloKitMobile
```

Если проект уже скачан:

```bash
git submodule update --init --recursive
```

Откройте проект в Android Studio с поддержкой AGP 9.4.0. Установите Android
SDK 37, NDK 28.2.13676358 и CMake 3.22.1. Используйте Gradle wrapper из
репозитория (9.6.0). Настройки Gradle daemon запрашивают JDK 25; Gradle может
загрузить этот toolchain. Совместимость исходного Java-кода задана на уровне 17.
Точные параметры находятся в
[настройках daemon](gradle/gradle-daemon-jvm.properties)
и [настройках приложения](app/build.gradle.kts).

Приложение работает на Android 7.0 и новее. Собираются версии для
`arm64-v8a`, `armeabi-v7a` и `x86_64`; QNN доступен только на arm64.

### Версия библиотеки

Приложение использует `includeBuild("vendor/YoloKit")`: оно собирается с
подмодулем в репозитории, а не с соседним клоном или скачанным AAR.
[Локальный README библиотеки](vendor/YoloKit/README.ru.md) описывает эту версию,
а новые изменения находятся в [репозитории YoloKit](https://github.com/gd182/YoloKit).

### Команды сборки

Добавьте модель и конфигурацию по инструкции ниже, затем соберите и установите APK:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Явная сборка нативного кода без QNN и проверка lint:

```bash
./gradlew -Pqnn.sdk.dir= :app:assembleDebug :app:lintDebug
```

[CI](.github/workflows/android.yml) выполняет эту сборку без QNN и сохраняет
debug APK как артефакт. Workflow не публикует релиз и не проверяет QNN
на телефоне. Для выпуска release APK нужна подпись:
[документация Android](https://developer.android.com/studio/publish/app-signing).

## Добавление моделей в APK

Модели читаются из `app/src/main/assets/`. Скопируйте шаблон конфигурации
и замените имена файлов и классов:

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

Например, для экспорта Ultralytics ncnn с двумя классами:

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

Положите `model.ncnn.param` и `model.ncnn.bin` рядом с `models.json`.
Названия классов должны соответствовать вашей модели, а не примеру.
Поле `default` содержит ID модели. Если оно отсутствует или такой модели
нет в доступном списке, выбирается первая.

`models.json` и файлы весов исключены из Git, но попадают в APK, если лежат
в assets во время сборки. Без локальной конфигурации приложение читает
[models.example.json](app/src/main/assets/models.example.json).
Это шаблон, а не готовая модель.

### Поля конфигурации

| Поле | Значение и значение по умолчанию |
|---|---|
| `id` | Идентификатор модели для `default` |
| `displayName` | Название в списке; по умолчанию `id` |
| `backend` | `ncnn` (по умолчанию) или `qnn` |
| `param` / `bin` | Обязательные имена файлов ncnn в assets |
| `model` | Обязательное имя файла контекста QNN в assets |
| `targetSize` | Размер входа ncnn при экспорте, положительный и кратный 32; по умолчанию 640 |
| `decoded` | Декодирует ли ncnn-граф рамки; по умолчанию `false` |
| `inputName` / `outputName` | `images` / `output` либо `in0` / `out0` при `decoded: true` |
| `bgr` | Порядок пикселей ncnn; по умолчанию `true`, для RGB укажите `false` |
| `confThreshold` / `nmsThreshold` | По умолчанию 0.25 / 0.45 |
| `labels` | Обязательный массив классов или имя файла, один класс на строку |
| `boxesNormalized` | Координаты QNN в диапазоне 0–1; по умолчанию `false` |
| `loadAsBytes` | Чтение ncnn-assets в `ByteArray`; по умолчанию `false` |

`targetSize` должен совпадать с `imgsz` при экспорте ncnn.
QNN читает фактическую форму входа из файла контекста.
Для Ultralytics ncnn явно укажите `decoded: true`, `bgr: false`,
`inputName: "in0"` и `outputName: "out0"`.

`loadAsBytes` показывает загрузку из памяти, например для модели после
расшифровки. Само приложение не шифрует модели, не читает внешние файлы
и не использует этот параметр для QNN-записей.

Команды экспорта и полные примеры для ncnn и QNN есть в
[README папки assets](app/src/main/assets/README.ru.md).
Поддерживаемые выходы описаны в
[требованиях YoloKit](vendor/YoloKit/README.ru.md#требования-к-модели).

## Работа с камерой

Разрешите доступ к камере и выберите модель из списка. Используется задняя
камера. Рядом с рамкой каждого объекта отображаются класс и уверенность.

Кнопка **Stop** выгружает детектор и для ncnn запрашивает освобождение ресурсов
Vulkan. Превью камеры продолжает работать. **Start** заново загружает ту же
модель, поэтому возобновление может занять время. При выборе другой модели
предыдущий детектор закрывается.

Статус показывает активный бэкенд и определённую разрядность. FPS считается
по обработанным кадрам, а время измеряет вызов `detect()`, не весь путь
от камеры до экрана.

## QNN на Snapdragon

Для QNN нужны нативная поддержка SDK и файл контекста под архитектуру HTP
телефона. Укажите QAIRT 2.50.0 в `vendor/YoloKit/local.properties`:

```properties
qnn.sdk.dir=/path/to/QAIRT/2.50.0
```

Вместо этого можно задать `QNN_SDK_ROOT` или передать Gradle
`-Pqnn.sdk.dir=/path/to/QAIRT/2.50.0`.
Каталог должен содержать `include/QNN/QnnInterface.h`.
Версии заголовков SDK, инструментов конвертации и runtime библиотеки
из Maven должны совпадать.

Добавьте запись с `backend: "qnn"`, `model` и правильными `labels`.
QNN-модели отображаются, если проверка устройства и сборки в YoloKit успешна.
Это не проверка совместимости конкретной модели: неподходящий файл контекста
всё равно может не загрузиться. Автоматического выбора по SoC нет.

Конвертация и настройка устройства:
[на русском](docs/QNN.ru.md) /
[in English](docs/QNN.md).

Скрипты:
[macOS и подключённый телефон](scripts/build_qnn_model.sh) /
[Ubuntu x86_64](scripts/build_qnn_model_ubuntu_x86.sh).
Параметры каждого скрипта доступны через `--help`.

## Если что-то не работает

| Проблема | Что проверить |
|---|---|
| `load failed` | Файлы модели в APK, имена, классы и настройки экспорта |
| QNN нет в списке моделей | arm64-устройство Qualcomm и QNN в сборке библиотеки |
| QNN-модель есть в списке, но не загружается | Runtime, архитектуру HTP и версию SDK файла контекста |
| Рамки смещены или классы неверные | Размер входа, формат экспорта, порядок цветов и классов |
| `restart failed` | Причину в статусе; детектор остаётся остановленным |

Логи:

```bash
adb logcat -s MainActivity YoloNcnn QnnYolo
```

## Код и связанные проекты

[MainActivity.kt](app/src/main/java/com/example/yolokitmobile/MainActivity.kt)
управляет CameraX, сменой моделей и потоком обработки.
[ModelRegistry.kt](app/src/main/java/com/example/yolokitmobile/ModelRegistry.kt)
преобразует JSON в конфигурацию библиотеки.
[OverlayView.kt](app/src/main/java/com/example/yolokitmobile/OverlayView.kt)
отображает координаты на обрезанном превью.

Кадры камеры преобразуются в ARGB_8888 Bitmap с учётом поворота.
YoloKit выполняет подготовку изображения, распознавание, декодирование и NMS
без OpenCV. API и подключение через AAR описаны в
[репозитории библиотеки](https://github.com/gd182/YoloKit)
и [инструкции AAR](https://github.com/gd182/YoloKit/blob/main/docs/AAR.ru.md).
Релизы библиотеки будут содержать AAR без весов моделей:
[YoloKit Releases](https://github.com/gd182/YoloKit/releases).

## Лицензия

Apache-2.0. Условия для проекта и сторонних компонентов приведены в
[LICENSE](LICENSE) и [NOTICE](NOTICE). У весов моделей свои лицензии.
