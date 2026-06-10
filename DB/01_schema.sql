-- =============================================================
--  員工座位系統 - DDL (資料表結構)
--  Database : PostgreSQL 17
--  說明     : 建立 SeatingChart(樓層座位表) 與 Employee(員工資料表)
-- =============================================================

-- 為求可重複執行，先移除既有物件 (順序：先子表再父表)
DROP TABLE IF EXISTS employee CASCADE;
DROP TABLE IF EXISTS seating_chart CASCADE;

-- -------------------------------------------------------------
-- SeatingChart 樓層座位表 (座位主檔)
--   FLOOR_SEAT_SEQ : 座位序號 (Primary Key, 自動遞增)
--   FLOOR_NO       : 樓層編號
--   SEAT_NO        : 座位編號
-- -------------------------------------------------------------
CREATE TABLE seating_chart (
    floor_seat_seq  SERIAL       PRIMARY KEY,
    floor_no        INTEGER      NOT NULL,
    seat_no         INTEGER      NOT NULL,
    CONSTRAINT uq_floor_seat UNIQUE (floor_no, seat_no)
);

COMMENT ON TABLE  seating_chart                IS '樓層座位表';
COMMENT ON COLUMN seating_chart.floor_seat_seq IS '座位序號 (PK)';
COMMENT ON COLUMN seating_chart.floor_no       IS '樓層編號';
COMMENT ON COLUMN seating_chart.seat_no        IS '座位編號';

-- -------------------------------------------------------------
-- Employee 員工資料表
--   EMP_ID         : 員編 (Primary Key, 固定 5 碼數字)
--   NAME           : 員工姓名
--   EMAIL          : 員工電子郵件
--   FLOOR_SEAT_SEQ : 座位資訊 (Foreign Key -> seating_chart)
--                    可為 NULL (代表尚未安排座位)
--                    UNIQUE        (代表一個座位最多只能被一位員工佔用)
-- -------------------------------------------------------------
CREATE TABLE employee (
    emp_id          CHAR(5)      PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    email           VARCHAR(100),
    floor_seat_seq  INTEGER,
    CONSTRAINT fk_employee_seat
        FOREIGN KEY (floor_seat_seq) REFERENCES seating_chart (floor_seat_seq),
    -- 一個座位最多一位員工 (NULL 不受 UNIQUE 限制，允許多位未就座員工)
    CONSTRAINT uq_employee_seat UNIQUE (floor_seat_seq),
    -- 員編固定 5 碼數字
    CONSTRAINT chk_emp_id CHECK (emp_id ~ '^[0-9]{5}$')
);

COMMENT ON TABLE  employee                IS '員工資料表';
COMMENT ON COLUMN employee.emp_id         IS '員編 (PK，固定 5 碼)';
COMMENT ON COLUMN employee.name           IS '員工姓名';
COMMENT ON COLUMN employee.email          IS '員工電子郵件';
COMMENT ON COLUMN employee.floor_seat_seq IS '座位資訊 (FK -> seating_chart)';

-- 加速以座位反查員工
CREATE INDEX idx_employee_seat ON employee (floor_seat_seq);
