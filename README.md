# 員工座位安排系統（Employee Seating System）

> 玉山銀行後端工程師實作題 — 人資部門使用的員工座位安排系統。
>
> 可瀏覽各樓層座位狀況、以下拉選單選員工後將其指派到空位、清除已佔用座位，最後一次性送出寫入資料庫。

本系統採 **Web Server + Application Server + 關聯式資料庫** 的三層式架構，後端再依「展示 / 業務 / 資料 / 共用」分層，所有資料庫存取一律透過 **Stored Procedure**，並針對 **SQL Injection** 與 **XSS** 做縱深防禦。

---

## 目錄

- [功能特色](#功能特色)
- [技術棧總覽](#技術棧總覽)
- [系統架構](#系統架構)
- [技術細節說明](#技術細節說明)
  - [前端技術](#前端技術)
  - [後端技術](#後端技術)
  - [資料庫技術](#資料庫技術)
- [專案結構](#專案結構)
- [資料庫設計](#資料庫設計)
- [Stored Procedure / Function](#stored-procedure--function)
- [RESTful API 文件](#restful-api-文件)
- [資訊安全措施](#資訊安全措施)
- [交易（Transaction）設計](#交易transaction設計)
- [環境需求](#環境需求)
- [安裝與啟動](#安裝與啟動)
- [操作說明](#操作說明)
- [設計決策與取捨](#設計決策與取捨)

---

## 功能特色

| 功能 | 說明 |
| --- | --- |
| 顯示各樓層座位 | 從資料庫讀取各樓層座位，依樓層分組呈現 |
| 三色狀態標示 | **灰**＝空位、**紅**＝已佔用（顯示員編）、**綠**＝請選擇（本次待送出的新指派） |
| 員工下拉選單 | 列出所有員工，員編固定 5 碼；並提示員工目前是否已就座 |
| 指派座位 | 選員工 → 點空位，該格變綠；每位員工只能佔一個座位（搬位會自動清空原位） |
| 清除座位 | 點選已佔用座位即可清為空位 |
| 批次送出 | 按「送出」一次性將所有異動寫入資料庫，全部成功或全部回滾 |
| 取消變更 | 送出前可一鍵還原回最初載入的狀態 |

---

## 技術棧總覽

| 層次 | 技術 | 版本 | 用途 |
| --- | --- | --- | --- |
| **前端框架** | [Vue.js](https://vuejs.org/) | 3.5.x | SPA 前端、響應式資料綁定（Composition API + `<script setup>`） |
| **前端建置工具** | [Vite](https://vitejs.dev/) | 5.4.x | 開發伺服器（HMR 熱更新）、production 打包、API proxy |
| **Vue 插件** | [@vitejs/plugin-vue](https://github.com/vitejs/vite-plugin-vue) | 5.x | 讓 Vite 編譯 `.vue` 單檔元件 |
| **HTTP 用戶端** | [Axios](https://axios-http.com/) | 1.x | 呼叫後端 RESTful API（Promise-based） |
| **後端框架** | [Spring Boot](https://spring.io/projects/spring-boot) | 3.3.5 | 應用程式骨架、IoC 容器、自動組態 |
| **Web 層** | spring-boot-starter-web | （隨 Boot） | 內嵌 Tomcat、Spring MVC、RESTful 控制器 |
| **資料存取** | spring-boot-starter-jdbc | （隨 Boot） | `JdbcTemplate` 呼叫 Stored Procedure |
| **參數驗證** | spring-boot-starter-validation | （隨 Boot） | Bean Validation（`@Valid` / `@Pattern` / `@NotBlank`） |
| **JSON 序列化** | Jackson | （隨 Boot） | 物件 ↔ JSON，並客製 XSS escape |
| **JDBC 驅動** | [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/) | （隨 Boot 管理） | 連接 PostgreSQL |
| **應用伺服器** | Apache Tomcat（內嵌） | 10.1.x | Servlet 容器（由 Spring Boot 內嵌啟動） |
| **執行環境** | Java（JDK） | 17 | 後端執行平台（LTS） |
| **建置工具** | [Apache Maven](https://maven.apache.org/) | 3.x | 依賴管理、編譯、打包成可執行 JAR |
| **資料庫** | [PostgreSQL](https://www.postgresql.org/) | 17 | 關聯式資料庫、PL/pgSQL Stored Procedure |
| **資料庫程式語言** | SQL / PL/pgSQL | — | DDL、DML、Function、Procedure |
| **前端執行環境** | [Node.js](https://nodejs.org/) | 18+（建議 LTS） | 跑 Vite 開發伺服器與建置 |
| **版本控制** | Git / GitHub | — | 原始碼版控 |

> 對應題目「技術要求」逐項：Vue.js ✅、Spring Boot ✅、RESTful API ✅、Maven ✅、Stored Procedure ✅、Transaction ✅、DDL/DML 放 `DB/` ✅、防 SQL Injection 與 XSS ✅。

---

## 系統架構

### 三層式架構（題目要求）

```
┌──────────────────────────────┐
│   Web Server / 瀏覽器          │   Vue 3 + Vite（http://localhost:5173）
│   - 座位圖 UI、下拉選單        │   ── 開發時 Vite proxy 將 /api 轉送後端
└───────────────┬──────────────┘
                │  HTTP / JSON（RESTful）
                ▼
┌──────────────────────────────┐
│   Application Server          │   Spring Boot + 內嵌 Tomcat（:8088）
│   - 展示 / 業務 / 資料 / 共用   │   ── 全程參數化、@Transactional
└───────────────┬──────────────┘
                │  JDBC（呼叫 Stored Procedure）
                ▼
┌──────────────────────────────┐
│   Relational Database         │   PostgreSQL 17
│   - 資料表 + Stored Procedure  │
└──────────────────────────────┘
```

### 後端四層分層（題目要求）

| 分層 | 套件 | 職責 | 對應類別 |
| --- | --- | --- | --- |
| **展示層** | `controller` | 收送 HTTP、繫結與基本驗證，不含商業邏輯 | `SeatController`、`EmployeeController` |
| **業務層** | `service` / `service.impl` | 商業邏輯、驗證、交易邊界（`@Transactional`） | `SeatServiceImpl`、`EmployeeServiceImpl` |
| **資料層** | `repository` | 透過 `JdbcTemplate` 呼叫 Stored Procedure | `SeatRepository`、`EmployeeRepository` |
| **共用層** | `common` | DTO、全域例外、CORS、XSS、Jackson 設定 | `ApiResponse`、`GlobalExceptionHandler`、`CorsConfig`、`HtmlCharacterEscapes` 等 |

設計原則：Controller 只管收送、Service 放邏輯與交易、Repository 集中所有 DB 存取、共用的東西放 common，彼此單向相依（controller → service → repository），方便測試與維護。

---

## 技術細節說明

### 前端技術

- **Vue 3（Composition API + `<script setup>`）**
  以 `ref` / `computed` 管理狀態，畫面隨資料自動更新。核心狀態包含：員工清單、座位佈局（工作中副本）、目前選擇的員工。座位狀態（空位 / 已佔用 / 請選擇）由 `computed` 即時推導，不需手動操作 DOM。
- **「工作中副本」模式**
  載入時同時保存每個座位的 `originalEmpId`，使用者的所有點選只改動副本；按「送出」時才比對原始 vs 工作中的差異，組成最小異動集合送往後端。如此可支援「取消變更」一鍵還原，也避免每次點擊就打 API。
- **Vite**
  提供 HMR 熱更新的開發伺服器（埠 5173），並透過 `server.proxy` 將 `/api` 代理到後端 `http://localhost:8088`，**前端因此與後端同源**，開發階段不需處理跨網域、瀏覽器也不會擋。
- **Axios**
  封裝在 `src/api/seating.js`，統一 `baseURL: '/api'` 與逾時設定，三支 API（取員工、取座位、送異動）集中管理。
- **XSS 防護（前端側）**
  Vue 樣板的 `{{ }}` 文字插值預設會跳脫 HTML，員工姓名等動態內容不會被當成 HTML/Script 執行。

### 後端技術

- **Spring Boot 3.3.5 / Java 17**
  以內嵌 Tomcat 啟動，免外部容器；`application.yml` 集中組態，連線資訊可用環境變數 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 覆寫（密碼不寫死進程式碼）。
- **Spring MVC（RESTful）**
  `@RestController` + `@RequestMapping("/api/...")`，回傳統一包成 `ApiResponse<T>`（`success` / `message` / `data`）。
- **Spring JDBC（`JdbcTemplate`）**
  刻意不使用 ORM，全部以 `JdbcTemplate` 呼叫 Stored Procedure（`SELECT * FROM fn_...()`、`CALL sp_...(?)`），符合題目「透過 Stored Procedure 存取資料庫」。
- **Bean Validation**
  `SeatAssignmentDTO` 上的 `@NotBlank`、`@Pattern(^[0-9]{5}$)` 在進入 Controller 時即驗證員編格式，`@Valid` 串連巢狀清單。
- **全域例外處理（`@RestControllerAdvice`）**
  將業務例外、驗證失敗、資料庫例外（含 Stored Procedure `RAISE EXCEPTION` 訊息如 `SEAT_OCCUPIED`）統一轉成友善的中文錯誤訊息與適當 HTTP 狀態碼，避免把底層堆疊或 SQL 細節洩漏給前端。
- **CORS 設定**
  `CorsConfig` 限定允許來源（預設 `http://localhost:5173`，可用 `CORS_ORIGINS` 覆寫），只開放必要的 HTTP 方法。

### 資料庫技術

- **PostgreSQL 17**
  以 `SERIAL` 自動產生座位序號；以 `UNIQUE` / `CHECK` / `FOREIGN KEY` 在資料庫層保證資料一致性。
- **PL/pgSQL Stored Procedure / SQL Function**
  讀取用 `LANGUAGE sql STABLE` 的 Function（`fn_get_seating_layout`、`fn_get_employees`），寫入用 `LANGUAGE plpgsql` 的 Procedure（`sp_apply_seat_assignments`），以 `JSONB` 接收批次異動。
- **可重複執行的腳本**
  `DB/` 內三支腳本皆以 `DROP ... IF EXISTS` / `CREATE OR REPLACE` / `TRUNCATE ... RESTART IDENTITY` 設計，可重複灌而不報錯。

---

## 專案結構

```
employee-seating-system/
├── DB/                                  資料庫腳本（題目要求放此資料夾）
│   ├── 01_schema.sql                    DDL：建立 seating_chart、employee
│   ├── 02_stored_procedures.sql         Function / Procedure
│   └── 03_sample_data.sql               DML：範例資料（對應題目範例畫面）
│
├── backend/                             Spring Boot 後端
│   ├── pom.xml                          Maven 專案描述、依賴
│   └── src/main/
│       ├── java/com/esunbank/seating/
│       │   ├── SeatingApplication.java          進入點
│       │   ├── controller/                      【展示層】
│       │   │   ├── SeatController.java
│       │   │   └── EmployeeController.java
│       │   ├── service/                          【業務層】介面
│       │   │   ├── SeatService.java
│       │   │   ├── EmployeeService.java
│       │   │   └── impl/                         業務層實作（交易控制）
│       │   │       ├── SeatServiceImpl.java
│       │   │       └── EmployeeServiceImpl.java
│       │   ├── repository/                       【資料層】呼叫 SP
│       │   │   ├── SeatRepository.java
│       │   │   └── EmployeeRepository.java
│       │   └── common/                           【共用層】
│       │       ├── dto/                          ApiResponse、各 DTO、請求物件
│       │       ├── exception/                    BusinessException、全域例外處理
│       │       └── config/                       CorsConfig、Jackson/XSS 設定
│       └── resources/
│           └── application.yml                   組態（埠、DB、CORS）
│
└── frontend/                            Vue 3 前端
    ├── package.json                     npm 依賴與指令
    ├── vite.config.js                   Vite 設定（埠、/api proxy）
    ├── index.html                       HTML 進入點
    └── src/
        ├── main.js                      掛載 Vue App
        ├── App.vue                      根元件
        ├── api/seating.js               Axios 封裝（呼叫後端）
        └── components/SeatingChart.vue  座位圖主元件
```

---

## 資料庫設計

### `seating_chart`（樓層座位表，座位主檔）

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `floor_seat_seq` | `SERIAL` PK | 座位序號（自動遞增） |
| `floor_no` | `INTEGER` | 樓層編號 |
| `seat_no` | `INTEGER` | 座位編號 |

- `UNIQUE (floor_no, seat_no)`：同一樓層的座位編號不重複。

### `employee`（員工資料表）

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `emp_id` | `CHAR(5)` PK | 員編（固定 5 碼數字） |
| `name` | `VARCHAR(50)` | 員工姓名 |
| `email` | `VARCHAR(100)` | 電子郵件 |
| `floor_seat_seq` | `INTEGER` FK | 座位（→ `seating_chart`，可為 NULL 代表未就座） |

- `FOREIGN KEY (floor_seat_seq)` → `seating_chart(floor_seat_seq)`。
- `UNIQUE (floor_seat_seq)`：**一個座位最多一位員工**（NULL 不受 UNIQUE 限制，故可有多位未就座者）。
- `CHECK (emp_id ~ '^[0-9]{5}$')`：員編固定 5 碼數字（資料庫層再擋一次）。

### 設計重點：座位資訊記在員工身上

座位的歸屬記在 `employee.floor_seat_seq`。因為員工這側只有一個座位欄位，天生就保證「**一人最多一位**」；再加上該欄 `UNIQUE`，又保證「**一位最多一人**」。兩個方向都由資料庫約束鎖死，不需靠應用程式自律。

---

## Stored Procedure / Function

| 名稱 | 語言 | 用途 |
| --- | --- | --- |
| `fn_get_seating_layout()` | SQL（STABLE） | 以 `LEFT JOIN` 撈各樓層座位（含坐在上面的員工，空位則員工欄為 NULL），依樓層、座位排序 |
| `fn_get_employees()` | SQL（STABLE） | 撈全部員工清單，供前端下拉選單 |
| `sp_apply_seat_assignments(p_assignments JSONB)` | PL/pgSQL | 批次套用座位異動，於同一交易內完成 |

`sp_apply_seat_assignments` 採 **兩階段（two-pass）** 處理：

1. **Pass 1**：先把這批所有受影響員工的座位清成 `NULL`。
2. **Pass 2**：再依目標座位重新指派。

這是為了避免「員工 A 要搬到員工 B 原本的位子、同批又把別人移過來」時，直接 `UPDATE` 會在中途撞到 `UNIQUE` 約束。先全部清空再重指派即可避開暫時衝突。過程中每筆都會檢查員編格式、員工存在、座位存在、座位未被批次外的人佔用，任何一筆 `RAISE EXCEPTION` 整批回滾。

---

## RESTful API 文件

所有回應統一格式：

```json
{ "success": true, "message": "success", "data": ... }
```

| Method | 路徑 | 說明 | 請求 Body |
| --- | --- | --- | --- |
| `GET` | `/api/employees` | 取得員工清單（下拉選單用） | — |
| `GET` | `/api/seats` | 取得各樓層座位佈局 | — |
| `POST` | `/api/seats/assignments` | 批次送出座位異動 | 見下 |

**`POST /api/seats/assignments` 請求範例**

```json
{
  "assignments": [
    { "empId": "12006", "floorSeatSeq": 5 },     // 指派 12006 到座位序號 5
    { "empId": "16142", "floorSeatSeq": null }   // 清除 16142 的座位
  ]
}
```

**錯誤回應範例**（HTTP 400）

```json
{ "success": false, "message": "座位已被其他員工佔用，請重新整理後再試", "data": null }
```

---

## 資訊安全措施

### SQL Injection 防護

- **全程參數化查詢**：所有 SQL 以 `?` 佔位、由 JDBC 綁定參數，**沒有任何字串拼接**。批次寫入以 `CALL sp_apply_seat_assignments(?::jsonb)` 傳入，資料當作參數值而非 SQL 片段。
- **員編三層驗證**：`^[0-9]{5}$` 在 **前端**、**後端 Service / Bean Validation**、**資料庫 CHECK / SP** 各擋一次，異常輸入無法穿透。
- **最小權限的錯誤訊息**：全域例外處理不回傳底層 SQL 或堆疊。

### XSS（跨站腳本）防護

- **後端輸出跳脫**：客製 `HtmlCharacterEscapes` 套進 Jackson，將 JSON 字串值中的 `<`、`>`、`&`、`'` 轉為 Unicode escape（`<` 等），輸出資料不會被當成 HTML/Script。
- **前端樣板跳脫**：Vue 的 `{{ }}` 文字插值本身就會跳脫 HTML。
- 兩端皆跳脫，形成縱深防禦。

---

## 交易（Transaction）設計

題目要求「需同時異動多個資料表時實作 Transaction」。本系統的批次座位異動可能一次更新多筆 `employee` 列：

- **業務層邊界**：`SeatServiceImpl.applyAssignments` 標註 `@Transactional`，整個方法在單一交易內執行。
- **資料庫層批次**：實際多筆 `UPDATE` 集中在 `sp_apply_seat_assignments` 內，由同一連線、同一交易處理。
- **全有或全無**：任何一筆驗證失敗或約束衝突（`RAISE EXCEPTION` / `UNIQUE` 違反）都會讓整批 rollback，不會出現「改了一半」的錯亂狀態。
- **送出前的業務驗證**：Service 在進 SP 前，先檢查員編格式、同批不可重複指派同一員工、同批不可重複指派同一座位，提早攔截。

---

## 環境需求

| 軟體 | 版本 |
| --- | --- |
| JDK | 17 以上 |
| Apache Maven | **選用**（專案已附 Maven Wrapper `mvnw`，沒裝 Maven 也能 build） |
| Node.js | 18 以上（建議 LTS） |
| PostgreSQL | 17（其他近版亦可） |

> 後端附了 **Maven Wrapper**（`backend/mvnw`、`backend/mvnw.cmd`），第一次執行會自動下載對應版本的 Maven，因此**評分者只要有 JDK 17 即可建置**，不需先安裝 Maven。下方指令把 `mvn` 換成 `./mvnw`（Windows 用 `.\mvnw.cmd`）即可。

---

## 安裝與啟動

### 1. 建立資料庫並灌入腳本

```powershell
# 建立資料庫
psql -U postgres -c "CREATE DATABASE seating_db;"

# 依序執行 DDL → Stored Procedure → 範例資料
psql -U postgres -d seating_db -f DB/01_schema.sql
psql -U postgres -d seating_db -f DB/02_stored_procedures.sql
psql -U postgres -d seating_db -f DB/03_sample_data.sql
```

### 2. 啟動後端（預設埠 8088）

連線資訊可用環境變數覆寫（**建議不要把密碼寫進設定檔**）：

```powershell
cd backend
$env:DB_PASSWORD = "你的資料庫密碼"   # 視需要再設 DB_URL / DB_USERNAME
.\mvnw.cmd spring-boot:run            # 或已裝 Maven 者：mvn spring-boot:run
```

或先打包成可執行 JAR 再啟動：

```powershell
cd backend
.\mvnw.cmd clean package              # 第一次會自動下載 Maven；macOS/Linux 用 ./mvnw
$env:DB_PASSWORD = "你的資料庫密碼"
java -jar target/employee-seating-system-1.0.0.jar
```

### 3. 啟動前端（預設埠 5173）

```powershell
cd frontend
npm install
npm run dev
```

打開瀏覽器進入 **http://localhost:5173** 即可使用。

> 後端使用 8088 是因為本機 8080 常被占用。若要改回 8080，調整 `backend/src/main/resources/application.yml` 的 `server.port` 與 `frontend/vite.config.js` 的 proxy `target` 即可。

---

## 操作說明

1. 從下拉選單選一位員工（會顯示員編、姓名與目前是否已就座）。
2. 點一個**空位（灰色）** → 該格變**綠色**，代表準備把這位員工放這。
3. 點**已就座的位子（紅或綠）** → 清空該座位。
4. 同一位員工搬到別的位子時，系統會自動清掉他原本的座位（一人一位）。
5. 確認無誤後按 **送出**，整批寫入資料庫；若中途想反悔，按 **取消變更** 還原。

座位顏色對照：**灰＝空位**、**紅＝已佔用**、**綠＝本次待送出的新指派（請選擇）**。

---

## 設計決策與取捨

- **座位資訊記在員工身上而非座位表**：讓「一人一位」與「一位一人」都由資料庫約束保證，應用程式不需額外維護一致性。
- **前端工作中副本 + 批次送出**：避免每次點擊就打 API、支援「取消變更」、把多筆異動收斂成一次交易，符合題目「按送出才寫入」的流程。
- **兩階段重指派的 Stored Procedure**：解決批次內座位互換造成的暫時 UNIQUE 衝突。
- **不使用 ORM、改用 `JdbcTemplate` + Stored Procedure**：直接對應題目「透過 Stored Procedure 存取資料庫」的要求，SQL 行為完全可控。
- **連線密碼走環境變數**：避免機敏資訊進入版控。
- **縱深防禦**：員編格式、SQL 參數化、XSS escape 都在多層各做一次，不依賴單一防線。
```