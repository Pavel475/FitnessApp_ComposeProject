# Отчёт по аудиту безопасности приложения (Лабораторная работа № 16)

## 1. Резюме
* **Область аудита:** мобильное приложение на Android (`com.example.fitnessapp_composeproject`, Kotlin / Jetpack Compose), ветка `vulnerable` (учебная сборка с намеренно внесёнными уязвимостями для проверки).
* **Методика оценки:** OWASP MASVS, CVSS v3.1 (обоснование базового вектора `AV/AC/PR/UI/S/C/I/A`), статический анализ (Android Lint, проверка зависимостей/SCA, поиск секретов в коде и истории Git) и динамический анализ (`adb`, `am start`, `logcat`, `mitmproxy`).
* **Итог находок:** **1 Critical**, **3 High**, **2 Medium** (всего 6 задокументированных находок; приоритеты триажа распределены на `P0`, `P1`, `P2` с учётом необходимости ротации секрета и закрытия до/в ходе релиза)[cite: 3].

---

## 2. Методика и воспроизводимые команды

Все проверки выполнялись в ветке `vulnerable` из корня проекта[cite: 3]:

```bash
# Статический анализ (Android Lint)
./gradlew lint

# Проверка зависимостей (SCA)
./gradlew app:dependencies --configuration releaseRuntimeClasspath

# Поиск секретов в коде и истории Git (gitleaks и/или git log)
gitleaks detect --source . --verbose
git log -p --all -S "AKIA_DEMO_NOTES_KEY" -- '*.kt' '*.xml' '*.kts'

# Динамический анализ: прямой вызов экспортированной активности и логкат
adb shell am start -n com.example.fitnessapp_composeproject/.ui.DeleteNoteActivity --es note_id "123"
adb logcat -d | grep -E "token=|AuthDebug|DeleteNoteActivity"

# Динамический анализ: резервная копия и локальное хранилище
adb backup -f backup.ab -noapk com.example.fitnessapp_composeproject
adb shell run-as com.example.fitnessapp_composeproject cat shared_prefs/auth_prefs.xml

# Перехват трафика (при настроенном прокси и mitmproxy -p 8888)
adb shell settings put global http_proxy 10.0.2.2:8888
```[cite: 3]

---

## 3. Находки аудита

### FIND-001: Вызов действия удаления через экспортированную `DeleteNoteActivity` без защиты прав
* **MASVS-группа:** `MASVS-PLATFORM-2` (взаимодействие с компонентами платформы и внешними вызовами)[cite: 3].
* **Серьёзность и CVSS:** **High — 8.2** (`CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:H/A:H`)[cite: 3].
  * *Обоснование вектора:* `AV:N` / `AC:L` / `PR:N` / `UI:N` — вызов активности возможен напрямую без взаимодействия пользователя и без предварительной аутентификации в рамках сценария; `I:H` / `A:H` — несанкционированное выполнение операции удаления затрагивает целостность и доступность данных[cite: 3].
* **Шаги воспроизведения:**
  1. Запустить приложение на эмуляторе/устройстве[cite: 3].
  2. Выполнить команду:
     ```bash
     adb shell am start -n com.example.fitnessapp_composeproject/.ui.DeleteNoteActivity --es note_id "123"
     ```
  3. Убедиться, что активность открывается и выполняет операцию удаления без проверки прав[cite: 3].
* **Доказательство:** Скриншот №2 (запуск `DeleteNoteActivity` через `adb shell am start` и уведомление об удалении заметки)[cite: 3].
* **Рекомендация:** Установить `android:exported="false"` для `DeleteNoteActivity` (или использовать собственный `signature`-permission и проверку сессии/прав внутри компонента)[cite: 3].

### FIND-002: Хранение токена в незашифрованном `SharedPreferences` при включённом резервном копировании
* **MASVS-группа:** `MASVS-STORAGE-1` (безопасное хранение конфиденциальных данных)[cite: 3].
* **Серьёзность и CVSS:** **High — 7.5** (`CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:C/C:H/I:N/A:N`)[cite: 3].
  * *Обоснование вектора:* локальный доступ к данным/бэкапу (`AV:L`, `AC:L`, `PR:L`, `UI:N`), раскрытие конфиденциального токена (`C:H`) при наличии резервного копирования (`allowBackup="true"`)[cite: 3].
* **Шаги воспроизведения:**
  1. Запустить код, записывающий `session_token` в `auth_prefs.xml`[cite: 3].
  2. Выполнить:
     ```bash
     adb backup -f backup.ab -noapk com.example.fitnessapp_composeproject
     adb shell run-as com.example.fitnessapp_composeproject cat shared_prefs/auth_prefs.xml
     ```
  3. Проверить содержимое файла `auth_prefs.xml`[cite: 3].
* **Доказательство:** Скриншот №4 (вывод `adb backup` и/или содержимое `shared_prefs/auth_prefs.xml` с токеном)[cite: 3].
* **Рекомендация:** Отключить резервное копирование (`android:allowBackup="false"`, `android:fullBackupContent="false"`, настроить `dataExtractionRules`), а секреты хранить в защищённом хранилище с ключами в Android Keystore / `EncryptedSharedPreferences`[cite: 3].

### FIND-003: Секрет / API-ключ в ресурсах приложения и истории Git
* **MASVS-группа:** `MASVS-STORAGE-1` (отсутствие захардкоженных секретов в коде, ресурсах и репозитории)[cite: 3].
* **Серьёзность и CVSS:** **Critical — 9.1** (`CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N`)[cite: 3].
  * *Обоснование вектора:* утечка ключа в репозитории и сборке при доступности кода/артефактов позволяет злоумышленнику использовать ключ в обход клиентского приложения (`AV:N`, `AC:L`, `PR:N`, `UI:N`, `C:H`, `I:H`)[cite: 3].
* **Шаги воспроизведения:**
  1. Выполнить поиск ключа в репозитории и истории Git:
     ```bash
     git log -p --all -S "AKIA_DEMO_NOTES_KEY" -- '*.kt' '*.xml' '*.kts'
     gitleaks detect --source . --verbose
     ```
  2. Убедиться, что ключ присутствует в ресурсах (`strings.xml`) и истории коммитов[cite: 3].
* **Доказательство:** Скриншот №5 (вывод `git log -p` / `gitleaks` с обнаруженным ключом)[cite: 3].
* **Рекомендация:** Выполнить ротацию ключа на стороне сервера (старый ключ отозвать/сделать недействительным), удалить секрет из исходников/ресурсов, передавать актуальные значения через защищённые переменные CI/CD и подключить `gitleaks` в pre-commit/CI[cite: 3].

### FIND-004: Логирование токена авторизации в системный журнал (`Log.d`)
* **MASVS-группа:** `MASVS-STORAGE-2` / `MASVS-CODE-4` (исключение утечки чувствительных данных в логи)[cite: 3].
* **Серьёзность и CVSS:** **Medium — 6.0** (`CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`)[cite: 3].
  * *Обоснование вектора:* локальный доступ к журналу логов (`AV:L`, `AC:L`, `PR:L`, `UI:N`) раскрывает значение токена (`C:H`)[cite: 3].
* **Шаги воспроизведения:**
  1. Запустить `DeleteNoteActivity`[cite: 3].
  2. Проверить логи:
     ```bash
     adb logcat -d | grep -E "token=|AuthDebug|DeleteNoteActivity"
     ```
* **Доказательство:** Скриншот №3 (строка `token=...` в выводе `logcat`)[cite: 3].
* **Рекомендация:** Удалить `Log.d` с токенами и секретами, использовать безопасное логирование без чувствительных полей и добавить проверку в Lint / Semgrep[cite: 3].

### FIND-005: Небезопасный `WebView` (`javascriptInterface` + `http://`) и cleartext-запрос к `api.notes.example`
* **MASVS-группа:** `MASVS-NETWORK-1` и `MASVS-PLATFORM-2` (шифрование сетевого канала и безопасная работа с `WebView`)[cite: 3].
* **Серьёзность и CVSS:** **High — 7.4** (`CVSS:3.1/AV:N/AC:H/PR:N/UI:R/S:U/C:H/I:H/A:N`)[cite: 3].
  * *Обоснование вектора:* перехват или подмена незащищённого `http://`-канала (`AV:N`, `AC:H`, `UI:R`) при наличии `JavascriptInterface` на странице по `http://` создаёт риск компрометации данных и внедрения скриптов в контекст моста (`C:H`, `I:H`)[cite: 3].
* **Шаги воспроизведения:**
  1. Проверить код `AuditVulnerableSamples.sendCleartextRequest()` и `AuditVulnerableSamples.setupInsecureWebView(...)`[cite: 3].
  2. Убедиться в наличии обращения по `http://api.notes.example` и привязки `JavascriptInterface` при загрузке по `http://`[cite: 3].
* **Доказательство:** Скриншот №6 (фрагмент кода `AuditVulnerableSamples.kt`)[cite: 3].
* **Рекомендация:** Использовать только `https://`, запретить cleartext через Network Security Config, убрать ненужный `JavascriptInterface` или ограничить его строго доверенным HTTPS-источником[cite: 3].

### FIND-006: Отключённая обфускация/оптимизация (`minifyEnabled = false`) и копирование в буфер обмена без очистки
* **MASVS-группа:** `MASVS-RESILIENCE-1` и `MASVS-STORAGE-1` (устойчивость к реверс-инжинирингу и защита чувствительных данных в буфере обмена)[cite: 3].
* **Серьёзность и CVSS:** **Medium — 5.3** (`CVSS:3.1/AV:L/AC:L/PR:L/UI:R/S:U/C:H/I:N/A:N`)[cite: 3].
  * *Обоснование вектора:* отсутствие R8/минификации облегчает анализ кода (`MASVS-RESILIENCE-1`), а копирование текста в буфер обмена без очистки оставляет данные доступными другим приложениям после завершения работы с экраном (`C:H`, `AV:L`, `UI:R`)[cite: 3].
* **Шаги воспроизведения:**
  1. Проверить `app/build.gradle.kts` (`isMinifyEnabled = false` для `release`) и `AuditVulnerableSamples.copyNoteToClipboardWithoutClear(...)`[cite: 3].
* **Доказательство:** Скриншот №1 (отчёт Lint) и Скриншот №6 (настройки `build.gradle.kts` и `ClipboardManager`)[cite: 3].
* **Рекомендация:** Включить `isMinifyEnabled = true` и `isShrinkResources = true` для `release` с правилами ProGuard/R8 для моделей/сериализации, добавить очистку буфера обмена через 30 секунд и `FLAG_SECURE` на экранах с чувствительными данными[cite: 3].

---

## 4. Триаж находок

| ID | Название находки | CVSS | Приоритет | Кто чинит | Когда чинить |
| :--- | :--- | :---: | :---: | :--- | :--- |
| `FIND-003` | Секрет / API-ключ в ресурсах и истории Git | `9.1` | **P0** | Разработчик + DevOps/Owner | **Немедленно: ротация ключа на сервере + вынос в секреты CI** (удаление коммита не лечит утечку)[cite: 3] |
| `FIND-001` | Вызов удаления через `exported="true"` `DeleteNoteActivity` | `8.2` | **P0** | Android-разработчик | **Немедленно, до релиза** (`exported=false` / `signature`-permission)[cite: 3] |
| `FIND-002` | Токен в `SharedPreferences` + `allowBackup="true"` | `7.5` | **P1** | Android-разработчик | **В текущем спринте** (запрет бэкапа + Keystore / шифрованное хранилище)[cite: 3] |
| `FIND-005` | `WebView` с `JavascriptInterface` и cleartext `http://` | `7.4` | **P1** | Android-разработчик | **В текущем спринте** (`https://`, NSC, ограничение `WebView`)[cite: 3] |
| `FIND-004` | Логирование токена в `Log.d` | `6.0` | **P2** | Android-разработчик | **Следующий спринт** (удаление логов токенов + правило Lint/Semgrep)[cite: 3] |
| `FIND-006` | `minifyEnabled=false` и буфер обмена без очистки | `5.3` | **P2** | Android-разработчик | **Следующий спринт** (R8 + ProGuard-правила + очистка буфера через 30 сек + `FLAG_SECURE`)[cite: 3] |

---

## 5. План hardening (переход в ветку `hardened`)

После фиксации доказательств в ветке `vulnerable` создаётся ветка `hardened` (`git checkout -b hardened`), где выполняются следующие изменения[cite: 3]:
1. **Манифест и бэкапы (`AndroidManifest.xml`):** `android:allowBackup="false"`, `android:fullBackupContent="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `DeleteNoteActivity` переводится в `android:exported="false"`[cite: 3].
2. **Сборка и R8 (`app/build.gradle.kts` и `proguard-rules.pro`):** включаются `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false` для `release`, добавляются правила сохранения аннотированных/сериализуемых моделей[cite: 3].
3. **Секреты и логи (`strings.xml`, `DeleteNoteActivity.kt`):** удаляется тестовый ключ из ресурсов, убирается `Log.d` с токеном, планируется ротация ключа на стороне сервера[cite: 3].
4. **Сеть, `WebView`, буфер обмена и RASP-минимум (`AuditVulnerableSamples.kt`, `IntegrityChecks.kt`, экраны):** запрещается `http://`, убирается небезопасный `JavascriptInterface` на `http://`, добавляется очистка буфера обмена через 30 секунд, `FLAG_SECURE` для экранов с чувствительными данными и базовые проверки отладчика/эмулятора (`isDebuggerAttached()`, `isEmulator()`)[cite: 3].
5. **Повторная проверка и diff:** повторный запуск команд аудита в ветке `hardened` и формирование `git diff vulnerable..hardened > hardening.diff`[cite: 3].
