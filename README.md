# 員工座位安排系統

人資部門用來安排員工座位的小系統。可以看各樓層座位狀況、把員工拖到空位、清空座位，最後一次送出存進資料庫。

前端 Vue 3、後端 Spring Boot、資料庫 PostgreSQL，資料存取都走 Stored Procedure。

## 用到的東西

- 前端：Vue 3 + Vite + Axios
- 後端：Spring Boot 3.3 / Java 17，Maven 建置
- 資料庫：PostgreSQL 17，透過 Spring JDBC 呼叫 Stored Procedure

## 專案結構

```
employee-seating-system/
├── DB/                  資料庫腳本
│   ├── 01_schema.sql            建表
│   ├── 02_stored_procedures.sql Stored Procedure / Function
│   └── 03_sample_data.sql       範例資料
├── backend/             Spring Boot
│   └── src/main/java/com/esunbank/seating/
│       ├── controller/  展示層，REST API
│       ├── service/     業務層，交易控制都在這
│       ├── repository/  資料層，呼叫 SP
│       └── common/      共用：DTO、例外、CORS、XSS 處理
└── frontend/            Vue
    └── src/
        ├── api/         打後端 API
        └── components/  座位圖
```

後端刻意拆成四層（展示 / 業務 / 資料 / 共用），對應題目的分層要求。Controller 只管收送，邏輯放 Service，碰資料庫的部分集中在 Repository，剩下共用的東西丟 common。

## 資料表

座位主檔 `seating_chart`：

| 欄位 | 說明 |
| --- | --- |
| floor_seat_seq | 座位序號（PK） |
| floor_no | 樓層 |
| seat_no | 座位編號 |

員工 `employee`：

| 欄位 | 說明 |
| --- | --- |
| emp_id | 員編（PK，5 碼） |
| name | 姓名 |
| email | Email |
| floor_seat_seq | 座位（FK，可為 NULL 代表沒座位） |

座位資訊是記在員工身上的。`floor_seat_seq` 設成 UNIQUE，這樣一個座位最多只會有一個人；員工這邊只有一個欄位，自然就保證一人一位。沒位子的人這欄是 NULL。

## Stored Procedure

| 名稱 | 做什麼 |
| --- | --- |
| `fn_get_seating_layout()` | 撈各樓層座位（含坐在上面的人） |
| `fn_get_employees()` | 撈員工清單給下拉選單用 |
| `sp_apply_seat_assignments(jsonb)` | 一次套用一批座位異動 |

`sp_apply_seat_assignments` 是寫入用的，整批在同一個交易裡跑。這邊踩過一個雷：如果 A 要搬到 B 原本的位子、同一批又把別人移過來，直接 update 會撞到 UNIQUE。所以拆成兩段做——先把這批要動到的人座位全清成 NULL，再重新指派，就不會在中途卡住。任何一筆出錯整批 rollback。

## API

| Method | 路徑 | 說明 |
| --- | --- | --- |
| GET | `/api/employees` | 員工清單 |
| GET | `/api/seats` | 各樓層座位 |
| POST | `/api/seats/assignments` | 送出座位異動 |

回傳統一長這樣：`{ "success": ..., "message": ..., "data": ... }`

## 資安

- SQL Injection：SQL 全部參數化（`?`）再丟進 Stored Procedure，沒有任何字串拼接。員編另外用 `^[0-9]{5}$` 在前端、Service、資料庫 CHECK 各擋一次。
- XSS：後端 Jackson 會把輸出字串裡的 `< > & '` 轉成 Unicode escape，前端 Vue 樣板插值本身也會跳脫，雙重保險。

## 怎麼跑起來

需要 JDK 17+、Maven、Node 18+、PostgreSQL 17。

**1. 建資料庫、灌腳本**

```powershell
psql -U postgres -c "CREATE DATABASE seating_db;"
psql -U postgres -d seating_db -f DB/01_schema.sql
psql -U postgres -d seating_db -f DB/02_stored_procedures.sql
psql -U postgres -d seating_db -f DB/03_sample_data.sql
```

**2. 後端**（預設 8088，連線資訊可用 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 環境變數蓋掉）

```powershell
cd backend
mvn spring-boot:run
```

**3. 前端**（預設 5173）

```powershell
cd frontend
npm install
npm run dev
```

開 http://localhost:5173 就能用了。

> 後端用 8088 是因為本機 8080 被其他服務占著，要改回去的話調 `backend/src/main/resources/application.yml` 跟 `frontend/vite.config.js` 的 proxy 即可。

## 操作

1. 下拉選一個員工。
2. 點空位（灰的）→ 那格變綠，表示要把這個人放這。
3. 點已經有人的位子（紅或綠）→ 清空。
4. 按送出，整批寫進資料庫。
