# Модели в assets

[English](README.md) · **Русский**

[Приложение](../../../../README.ru.md) читает из этой папки конфигурацию,
названия классов и файлы моделей. Модели включаются в APK при сборке.
Загрузки из интернета и импорта файлов в установленном приложении нет.

В Git хранятся [models.example.json](models.example.json) и [labels.txt](labels.txt).
Рабочие `models.json`, `*.param` и `*.bin` исключены из Git, но это не исключает
их из APK. Веса нужно положить сюда до сборки.

Все команды ниже выполняются из корня YoloKitMobile.

## Конфигурация

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

Без `models.json` приложение использует файл-пример. Его имена и классы —
заглушки: без соответствующих файлов распознавание покажет `load failed`.

У каждой модели должны быть `id`, нужные файлы и `labels`.
Поле `default` ссылается на `id` стартовой модели. В `labels` можно записать
массив классов или имя текстового файла в assets, один класс на строку:

```json
{
  "labels": "labels.txt"
}
```

Это пример поля, а не полная запись модели.

Файл `labels.txt` содержит классы COCO. Используйте его только для модели,
обученной на тех же 80 классах. Для своей модели сохраните собственные названия
в порядке обучения. Не подменяйте этот порядок алфавитной сортировкой.

Полный список полей и значений по умолчанию есть в
[README приложения](../../../../README.ru.md#поля-конфигурации).

## Экспорт ncnn

Для модели детекции с совместимым выходом Ultralytics:

```bash
python -m pip install ultralytics
yolo export model=best.pt format=ncnn imgsz=640
cp best_ncnn_model/model.ncnn.param best_ncnn_model/model.ncnn.bin app/src/main/assets/
```

Параметры экспорта описаны в
[документации Ultralytics](https://docs.ultralytics.com/integrations/ncnn/).
Поддерживаемые выходы — в
[README YoloKit](../../../../vendor/YoloKit/README.ru.md#требования-к-модели).
Не каждый экспорт с расширением ncnn подходит: API рассчитан на детекцию,
а не сегментацию, классификацию или оценку позы.

Пример полного `models.json` для модели с двумя классами:

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

Замените `labels` на классы своей модели; порядок можно сверить с
`best_ncnn_model/metadata.yaml`. Укажите `targetSize`, равный `imgsz` при экспорте,
и явно задайте `bgr: false` для RGB. Несовпадение размера, цветового порядка
или названий тензоров может дать неверные результаты.

Для сырого YOLOv8 DFL обычно нужны `decoded: false`, `inputName: "images"`,
`outputName: "output"` и `bgr: true`. Это другой формат выхода; настройки
нужно выбирать по графу модели, а не по расширению файлов.

### Загрузка из байтов

В ncnn-запись можно добавить `"loadAsBytes": true`. Реестр прочитает те же
assets в массивы и передаст их в YoloKit. Это демонстрация загрузки из памяти:
файлы не шифруются и не становятся внешними по отношению к APK.

Параметр не используется для QNN-записей.

## QNN

Сначала соберите файл контекста QNN под архитектуру HTP телефона и версию runtime.
Инструкции находятся в [QNN.ru.md](../../../../docs/QNN.ru.md) и
[QNN.md](../../../../docs/QNN.md). Оба скрипта по умолчанию сохраняют результат
в эту папку:

- [macOS / подключённый телефон](../../../../scripts/build_qnn_model.sh);
- [Ubuntu x86_64](../../../../scripts/build_qnn_model_ubuntu_x86.sh).

Пример конфигурации для файла `model_v81.bin` с двумя классами:

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

Замените классы и имя файла на свои. `boxesNormalized: true` нужен только
для координат в диапазоне 0–1. Размер и тип входа QNN читаются из файла
контекста; разрядность вручную задавать не нужно.

Появление QNN в списке моделей ещё не подтверждает совместимость файла контекста.
Приложение не выбирает модель по SoC автоматически. На устройстве, где
проверка QNN не проходит, эти записи скрыты, поэтому для работы на разных
телефонах добавьте также ncnn-модель.

## Сборка и проверка

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s MainActivity YoloNcnn QnnYolo
```

При `load failed` проверьте имена и наличие файлов в APK.
Если рамки смещены, проверьте формат экспорта и размер входа; если классы
неверные — число и порядок названий. При подготовке релизного APK проверьте,
какие именно локальные модели и конфигурация попали в assets.

Условия лицензий приведены в [LICENSE](../../../../LICENSE)
и [NOTICE](../../../../NOTICE). Лицензия исходного кода не задаёт лицензию
добавленных весов.
