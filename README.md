# Champion Guides

Android-застосунок для офлайн-перегляду довідника та порівняння збірок. У каталозі можна шукати персонажів, сортувати список, читати картки, порівнювати до трьох збірок і зберігати обране на пристрої. Типова збірка та GitHub Actions використовують вигаданий тестовий каталог.

## Збірка

Потрібні JDK 17, Android SDK Platform 37 та Build Tools 36.0.0. Застосунок підтримує Android 9 і новіші версії. Gradle Wrapper завантажує Gradle 9.6.0 і перевіряє контрольну суму дистрибутива.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

На Linux або macOS виконайте `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

Локальний APK знаходиться в `app/build/outputs/apk/debug/app-debug.apk`. Це збірка для перевірки. Публічний реліз потребує постійного ключа підпису.

## Дані та профіль

- Тестовий каталог зберігається в `app/src/main/assets/catalog.json`. Парсер перевіряє версію схеми, унікальність ідентифікаторів і посилання на реліквії.
- Обране зберігається в окремій базі Room `profile.db`. Схема профілю експортується в `app/schemas/`.
- Підписані оновлення каталогу й хмарна синхронізація заплановані для наступних етапів.

## Локальна збірка зі своїм каталогом

Із кореня цього Android-проєкту експортуйте знімок сусідньої бібліотеки в новий локальний каталог. Експортер читає `database.json` і портрети. Для локальної збірки Gradle бере цей експорт замість тестових даних.

```powershell
python -B .\tools\export_local_catalog.py .. ..\.android-toolchain\catalog-local
.\gradlew.bat "-PcatalogDir=..\.android-toolchain\catalog-local" :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Локальний APK має ідентифікатор `app.championguides.local` та окрему базу обраного. З параметром `catalogDir` release-варіант вимкнений. Знімок і APK з ігровим контентом призначені для локальної перевірки. Посилання на зовнішні джерела відкриваються у браузері, а картки, портрети та описи реліктів читаються офлайн.
