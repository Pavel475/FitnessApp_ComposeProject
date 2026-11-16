## Лабораторная работа № 14. Контроль доступа (RBAC, Permission Flow, Deep Links, Аудит)

### 1. Реализация четырёх звеньев контроля доступа

| Звено контроля доступа | Назначение | Реализация в проекте |
| :--- | :--- | :--- |
| **1. Идентификация** | Заявление субъекта о себе (ввод логина / идентификатора пользователя) | `LoginScreen.kt`, передача `userId` (`pavel`, `guard`, `admin`) в `AuthViewModel.login()` |
| **2. Аутентификация** | Проверка подлинности субъекта по факторам (пароль, локальный PIN, recovery-код) | `AuthRepository.login()`, `PinVault` (SHA-256 + соль), `SessionManager` (сессия в RAM) |
| **3. Авторизация (RBAC + Scopes)** | Определение разрешённых действий на основе роли и серверных `scope` из JWT | `Role.kt` (`Role`, `Permission`, `AccessContext`), `AuthRepository` (выдача claims), трёхуровневая проверка в UI (`MainRoot`, `ScanScreen`), `AccessViewModel` и мок-сервере (`executeServerScan`, `fetchServerAudit`)[cite: 4] |
| **4. Аудит безопасности** | Неизменяемая (append-only) фиксация всех решений доступа (`ALLOWED` / `DENIED`) с timestamp | `AuditRepository.kt` (запись JSONL в `filesDir/access_audit.log`), просмотр в `AdminScreen.kt` (только при наличии scope `audit:read`)[cite: 4] |

---

### 2. Архитектура ролей и трёхуровневая проверка прав

Клиентская проверка роли используется исключительно для UX (скрытие недоступных вкладок и кнопок без отображения агрессивных ошибок), а источником истины выступают серверные `scope`[cite: 4]:

* **`EMPLOYEE` (`pavel` / `password123`)** — scopes: `["pass:view"]`[cite: 4]. Доступен экран пропуска (`PassScreen`); на экране `ScanScreen` кнопка сканирования скрыта; вкладка журнала доступа в навигации отсутствует[cite: 4].
* **`SECURITY` (`guard` / `guard123`)** — scopes: `["pass:view", "scan:execute"]`[cite: 4]. Доступен экран пропуска и сканер пропусков после прохождения runtime-запроса разрешения `CAMERA`[cite: 4].
* **`ADMIN` (`admin` / `admin123`)** — scopes: `["pass:view", "scan:execute", "audit:read"]`[cite: 4]. Полный доступ, включая вкладку «Журнал» (`AdminScreen`) для чтения записей `access_audit.log`[cite: 4].

**Три уровня защиты:**
1. **UI-уровень (`MainRoot`, `ScanScreen`, `AdminScreen`):** элементы интерфейса отображаются только при выполнении `accessContext.can(Permission)`[cite: 4].
2. **Уровень ViewModel (`AccessViewModel`):** методы `onScanRequested()` и `loadAuditLogs()` повторно проверяют права и фиксируют попытку обхода (`DENIED`) в `access_audit.log`[cite: 4].
3. **Уровень «сервера» (`AuthRepository`):** методы `executeServerScan()` и `fetchServerAudit()` проверяют наличие нужного `scope` в контексте токена и отклоняют запрос с `SecurityException` при его отсутствии[cite: 4].

---

### 3. Безопасный Permission Flow и защита компонентов

* **Runtime-разрешение `CAMERA` (`PermissionFlow.kt`):**
    * В `AndroidManifest.xml` объявлено только необходимое разрешение `android.permission.CAMERA` (без неиспользуемых разрешений)[cite: 4].
    * Перед системным запросом пользователю отображается карточка-обоснование (**rationale**), объясняющая цель использования камеры[cite: 4].
    * При отказе или выборе «Не спрашивать снова» интерфейс не падает и предлагает кнопку прямого перехода в системные настройки приложения (`ACTION_APPLICATION_DETAILS_SETTINGS`) с автоматическим обновлением статуса по событию жизненного цикла `ON_RESUME`[cite: 4].
* **Экспортированные компоненты и Deep Links (`AndroidManifest.xml`, `InviteActivity.kt`):**
    * Внешний вход через Deep Link (`https://passes.company.example/invite?token=...`) изолирован в `InviteActivity` (`android:exported="true"`)[cite: 4].
    * Параметр `token` проходит строгую валидацию по регулярному выражению `^[A-Za-z0-9_-]{8,64}$` до любого использования[cite: 4]. Подделанные или пустые ссылки отклоняются с выводом экрана ошибки и записью события `DENIED` в аудит[cite: 4].

---

### 4. Команды проверки через `adb` и ответы к защите

**Тестирование Deep Link и защиты компонентов:**
```bash
# 1. Валидная ссылка-приглашение (открывает подтверждение приглашения)
adb shell am start -a android.intent.action.VIEW -d "[https://passes.company.example/invite?token=VALIDTOKEN123](https://passes.company.example/invite?token=VALIDTOKEN123)"

# 2. Подделанный/короткий токен (отклоняется с понятным сообщением об ошибке и записью DENIED в аудит)
adb shell am start -a android.intent.action.VIEW -d "[https://passes.company.example/invite?token=bad](https://passes.company.example/invite?token=bad)"