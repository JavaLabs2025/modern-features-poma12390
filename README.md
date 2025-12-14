# Workflows и команды для тестирования (Java 26 + Gradle + preview)

Этот файл предназначен для демонстрации и проверки лабораторной работы: запуск тестов, прогон сценариев CLI, проверка прав/инвариантов и диагностика типовых проблем (кодировка, preview).

---

## 1) Быстрый старт: один прогон “всё работает”

### PowerShell
```powershell
./gradlew clean test
./gradlew -q run
```

### CMD (рекомендуется для корректной кодировки в консоли)
```bat
chcp 65001
.\gradlew clean test
.\gradlew -q run
```

---

## 2) Тесты JUnit (Gradle)

### Запуск всех тестов
```powershell
./gradlew test
```

### Чистый прогон (полезно, если что-то “кешируется”)
```powershell
./gradlew clean test
```

### Больше деталей (помогает при падениях)
```powershell
./gradlew test --info
./gradlew test --stacktrace
```

### Запуск конкретного тест-класса (JUnit Platform)
(Работает, если у тебя стандартная структура `src/test/java` и JUnit 5.)
```powershell
./gradlew test --tests "org.lab.*"
```

---

## 3) Запуск приложения (CLI) через Gradle

### Запуск CLI
```powershell
./gradlew run
```

### Запуск без “шума” Gradle (удобно на демо)
```powershell
./gradlew -q run
```

### Диагностика падений приложения
```powershell
./gradlew run --stacktrace
./gradlew run --info
```

---


---

## Команды CLI: полный список, описание и примеры

Ниже перечислены **все команды**, которые поддерживает CLI, с кратким назначением, синтаксисом и примером.

### Общие команды

#### `help`
Показывает справку по командам и форматам ссылок (`lastProject`, `lastTicket`, и т.д.).

**Пример**
```text
help
```

#### `demo`
Запускает встроенный демонстрационный сценарий (последовательность команд).

**Пример**
```text
demo
```

#### `exit`
Завершает работу CLI.

**Пример**
```text
exit
```

---

### Пользователи

#### `register <login> "Display Name"`
Регистрирует пользователя в системе и привязывает `login` к созданному `userId`.

**Пример**
```text
register manager "Project Manager"
register dev "Backend Developer"
register tester "QA Tester"
```

---

### Проекты

#### `create-project <actorLogin> "Project Name" "Description"`
Создаёт новый проект от имени пользователя `actorLogin`. Создатель становится менеджером проекта.

**Ссылки на проект (`projectRef`)**
- ключ проекта (например, `PRJ-000001`)
- UUID проекта
- `lastProject` или `last`

**Пример**
```text
create-project manager "Demo Project" "Project created from CLI"
```

#### `dashboard <actorLogin>`
Показывает Dashboard пользователя: проекты, тикеты, баги “к исправлению/проверке”.
Внутри сервиса Dashboard собирается параллельно (structured concurrency, preview).

**Пример**
```text
dashboard manager
dashboard dev
dashboard tester
```

---

### Участники проекта и роли (обычно менеджер)

#### `add-dev <actorLogin> <projectRef> <memberLogin>`
Добавляет пользователя `memberLogin` в проект как разработчика.

**Пример**
```text
add-dev manager lastProject dev
```

#### `add-tester <actorLogin> <projectRef> <memberLogin>`
Добавляет пользователя `memberLogin` в проект как тестировщика.

**Пример**
```text
add-tester manager lastProject tester
```

---

### Milestone’ы

#### `create-milestone <actorLogin> <projectRef> "Milestone Name" <start yyyy-mm-dd> <end yyyy-mm-dd>`
Создаёт milestone в проекте с указанным диапазоном дат.

**Пример**
```text
create-milestone manager lastProject "Milestone 1" 2025-12-14 2025-12-21
```

#### `activate-milestone <actorLogin> <projectRef> <milestoneRef>`
Переводит milestone в статус ACTIVE.

**Ссылки на milestone (`milestoneRef`)**
- UUID milestone
- `lastMilestone` или `last`

**Пример**
```text
activate-milestone manager lastProject lastMilestone
```

---

### Ticket’ы

#### `create-ticket <actorLogin> <projectRef> <milestoneRef> "Title" "Description"`
Создаёт тикет в milestone проекта.

**Пример**
```text
create-ticket manager lastProject lastMilestone "Implement feature A" "Implement business logic A"
```

#### `assign-ticket <actorLogin> <projectRef> <ticketRef> <developerLogin>`
Назначает разработчика на тикет.

**Ссылки на ticket (`ticketRef`)**
- UUID тикета
- `lastTicket` или `last`

**Пример**
```text
assign-ticket manager lastProject lastTicket dev
```

#### `start-ticket <actorLogin> <projectRef> <ticketRef>`
Начинает выполнение тикета. В зависимости от текущего статуса может сначала принять тикет (если у тебя реализовано “доведение” статусов в CLI).

**Пример**
```text
start-ticket dev lastProject lastTicket
```

#### `done-ticket <actorLogin> <projectRef> <ticketRef>`
Завершает тикет (DONE). Аналогично может “доводить” тикет через промежуточные статусы, если так реализовано в CLI.

**Пример**
```text
done-ticket dev lastProject lastTicket
```

---

### Bug Report’ы

#### `create-bug <actorLogin> <projectRef> "Title" "Description"`
Создаёт bug report в проекте.

**Пример**
```text
create-bug tester lastProject "Bug A" "Found defect during testing"
```

#### `fix-bug <actorLogin> <projectRef> <bugRef>`
Помечает bug report как FIXED (обычно делает разработчик).

**Ссылки на bug (`bugRef`)**
- UUID bug report
- `lastBug` или `last`

**Пример**
```text
fix-bug dev lastProject lastBug
```

#### `test-bug <actorLogin> <projectRef> <bugRef>`
Помечает bug report как TESTED (обычно делает тестировщик).

**Пример**
```text
test-bug tester lastProject lastBug
```

#### `close-bug <actorLogin> <projectRef> <bugRef>`
Закрывает bug report (CLOSED), обычно после TESTED.

**Пример**
```text
close-bug tester lastProject lastBug
```

---

### Примечания по ссылкам (refs): `lastProject`, `lastTicket`, …

CLI поддерживает короткие ссылки на “последние” созданные сущности в рамках текущего процесса.

- `lastProject` / `last` — последний созданный проект
- `lastMilestone` / `last` — последний milestone в рамках `lastProject` (или текущего `projectRef`)
- `lastTicket` / `last` — последний тикет в рамках проекта
- `lastBug` / `last` — последний bug report в рамках проекта

Для точности на демонстрации можно всегда использовать **UUID/KEY** из вывода CLI вместо `last*`.


## 4) Workflow: Happy-path end-to-end (manager → dev → tester → dashboard)

Вставляй команды по одной в CLI после запуска `./gradlew run`.

```text
register manager "Project Manager"
register dev "Backend Developer"
register tester "QA Tester"

create-project manager "Demo Project" "Project created from workflow"
dashboard manager

add-dev manager lastProject dev
add-tester manager lastProject tester

create-milestone manager lastProject "Milestone 1" 2025-12-14 2025-12-21
activate-milestone manager lastProject lastMilestone

create-ticket manager lastProject lastMilestone "Implement feature A" "Implement business logic A"
assign-ticket manager lastProject lastTicket dev

dashboard dev

start-ticket dev lastProject lastTicket
done-ticket dev lastProject lastTicket

create-bug tester lastProject "Bug A" "Found defect during testing"
fix-bug dev lastProject lastBug
test-bug tester lastProject lastBug
close-bug tester lastProject lastBug

dashboard manager
```

Что проверяет:
- регистрацию
- создание проекта
- назначение участников
- milestone lifecycle (создание/активация)
- ticket lifecycle (назначение/выполнение)
- bug lifecycle (создание/исправление/проверка/закрытие)
- dashboard (сборка данных параллельно внутри сервиса)

---

## 5) Workflow: проверка прав (ожидаемые ACCESS_DENIED)

```text
register manager "Project Manager"
register dev "Backend Developer"
register tester "QA Tester"

create-project manager "Security Demo" "Access control checks"
add-dev manager lastProject dev
add-tester manager lastProject tester

create-milestone manager lastProject "Iter" 2025-12-14 2025-12-20
activate-milestone manager lastProject lastMilestone
create-ticket manager lastProject lastMilestone "Sec ticket" "Ticket for access checks"
assign-ticket manager lastProject lastTicket dev
```

Негативные попытки (часть должна отказать по ролям):
```text
add-dev dev lastProject tester
create-milestone dev lastProject "Should fail" 2025-12-14 2025-12-15
assign-ticket tester lastProject lastTicket tester
```

Ожидаемо: Failure с типом `ACCESS_DENIED` (или иной конкретный FailureCause, если у тебя правила иные).

---

## 6) Workflow: статус-машина тикета (NEW → ACCEPTED → IN_PROGRESS → DONE)

```text
register manager "Project Manager"
register dev "Backend Developer"

create-project manager "Ticket State Machine" "Show transitions"
add-dev manager lastProject dev

create-milestone manager lastProject "Iter" 2025-12-14 2025-12-16
activate-milestone manager lastProject lastMilestone

create-ticket manager lastProject lastMilestone "State demo ticket" "Observe transitions"
assign-ticket manager lastProject lastTicket dev

dashboard dev
start-ticket dev lastProject lastTicket
dashboard dev
done-ticket dev lastProject lastTicket
dashboard dev
```

---

## 7) Workflow: статус-машина баг-репорта (NEW → FIXED → TESTED → CLOSED)

```text
register manager "Project Manager"
register dev "Backend Developer"
register tester "QA Tester"

create-project manager "Bug Lifecycle Project" "Show bug transitions"
add-dev manager lastProject dev
add-tester manager lastProject tester

create-bug tester lastProject "Bug lifecycle demo" "Bug created for transition demo"
dashboard tester

fix-bug dev lastProject lastBug
dashboard dev

test-bug tester lastProject lastBug
close-bug tester lastProject lastBug

dashboard manager
```

---

## 8) Workflow: доменные ошибки/валидации (NotFound, invalid range)

### NotFound (вставь заведомо несуществующие UUID)
```text
register manager "Project Manager"

activate-milestone manager 00000000-0000-0000-0000-000000000000 00000000-0000-0000-0000-000000000000
done-ticket manager 00000000-0000-0000-0000-000000000000 00000000-0000-0000-0000-000000000000
close-bug manager 00000000-0000-0000-0000-000000000000 00000000-0000-0000-0000-000000000000
```

### Неверный диапазон дат milestone (если домен валидирует start <= end)
```text
register manager "Project Manager"
create-project manager "Invalid milestone dates" "Range check"
create-milestone manager lastProject "Bad range" 2025-12-20 2025-12-14
```

---

## 9) Dashboard как демонстрация structured concurrency (preview)

Сценарий, насыщенный данными, чтобы показать параллельную сборку:
```text
register manager "Project Manager"
register dev "Backend Developer"
register tester "QA Tester"

create-project manager "Concurrency Dashboard" "Fill data and query dashboards"
add-dev manager lastProject dev
add-tester manager lastProject tester

create-milestone manager lastProject "Iter" 2025-12-14 2025-12-31
activate-milestone manager lastProject lastMilestone

create-ticket manager lastProject lastMilestone "Ticket #1" "Work item 1"
assign-ticket manager lastProject lastTicket dev
start-ticket dev lastProject lastTicket
done-ticket dev lastProject lastTicket

create-bug tester lastProject "Bug #1" "First bug"
fix-bug dev lastProject lastBug

dashboard manager
dashboard dev
dashboard tester
```

---

## 10) Прогон сценария из файла (без ручного ввода)

### Windows CMD
1) Создай файл `scenario.txt` в корне проекта, по одной команде на строку.
2) Запусти:
```bat
type scenario.txt | .\gradlew -q run
```

### PowerShell
```powershell
Get-Content .\scenario.txt | ./gradlew -q run
```

---

## 11) Типовые проблемы и быстрые решения

### 11.1 “preview feature … disabled by default”
Проверь, что в Gradle для **compileJava/test/run** добавлен `--enable-preview`.

Проверка:
```powershell
./gradlew -q tasks
```

И запуск с подробностями:
```powershell
./gradlew run --info
./gradlew test --info
```

### 11.2 “DEMO ��������” (битая кодировка)
CMD:
```bat
chcp 65001
.\gradlew -q run
```

И/или JVM-аргумент `-Dfile.encoding=UTF-8` должен быть добавлен в JavaExec/Test.

### 11.3 В CLI “после команд ничего не печатается”
Это означает, что CLI не печатает результат `Result<String>` после выполнения команды.
Исправление: в CLI после `runner.execute(cmd)` обязательно делать `System.out.println(...)` для Success/Failure.

---

## 12) Мини-чеклист перед демонстрацией преподавателю

1) `./gradlew clean test` — зелёный.
2) `./gradlew -q run` — CLI запускается без падений.
3) Вставить workflow из раздела 4 — должны быть видны:
   - создание проекта
   - создание milestone
   - создание ticket
   - создание bug
   - dashboard
4) Запустить workflow из раздела 5 — показать отказ по ролям.
5) Показать `dashboard` как пример structured concurrency.

