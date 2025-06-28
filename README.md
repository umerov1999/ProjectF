# Fenrir VK
Первый языческий<br>

За основу взят проект Phoenix (R)<br>

<b>Языки: Русский, Английский</b>

Многие существующие моды и т.д. построены на официальном приложении. Этот клиент написан с нуля, поэтому многих привычных функций не будет, либо будут выглядить иначе.<br>

<b>Скриншоты:</b>

<img src="Fenrir_VK.jpg" alt=""/>

<b>Инструкция по сборке:</b><br>
Требуется:<br>
  1. Android Studio Narwhal 2025.1.1 или выше. Kotlin 2.2.*<br>
  2. Android SDK 36<br>
  3. Android NDK 29.0.13599879<br>
  4. CMake 4.0.2<br>
  
  Если не работает музыка в Fenrir Kate, обновите kate_receipt_gms_token в app.build_config.<br>
  Взять токен можно из Kate Mobile Extra Mod<br>
  
<b>Компиляция:</b>

  1. Для релизных сборок вам нужен сертификат.<br>
  keytool -genkey -v -keystore Fenrir.keystore -alias fenrir -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 10000<br>
  2. Вариант 1. Далее нужно собрать нативную библиотеку:<br>
  Создать папку compiled_native, раскомментировать [include ":native"] в settings.gradle<br>
  cd native<br>
  ./ffmpeg.sh<br>
  после синхронизации репозитория ffmpeg введите min sdk version<br>
  после сборки ffmpeg соберите native в Android Studio и поместите native-release.aar в compiled_native<br>
  3. Вариант 2. Далее нужно собрать нативную библиотеку:<br>
  Раскомментировать [include ":native"] в settings.gradle<br>
  Раскомментировать [implementation project(":native")] в app*/build.gradle<br>
  Удалить [implementation fileTree('../compiled_native') { include '*.aar' }] в app*/build.gradle<br>
  cd native<br>
  ./ffmpeg.sh<br>
  после синхронизации репозитория ffmpeg введите min sdk version<br>
  после сборки ffmpeg можете собирать проект<br>
  4. Выберите flavor - fenrir или kate и Debug или Release и соберите apk :)<br>

Локальный медиа сервер https://github.com/umerov1999/FenrirMediaServer/releases <br>

<b>Старые репозитории:</b>

  1. https://github.com/umerov1999/Old_Fenrir-for-VK Release 1<br>
  2. https://github.com/umerov1999/Old2_Fenrir-for-VK Release 2<br>
  3. https://github.com/umerov1999/Fenrir-for-VK Финальный релиз<br>

# FileGallery
Просмотр фото, видео, аудио, тэги<br>

<b>Языки: Русский, Английский</b>

<b>Скриншот:</b>

<img src="FileGallery.jpg" alt=""/>
