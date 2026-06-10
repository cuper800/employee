-- =============================================================
--  員工座位系統 - Stored Procedures / Functions
--  Database : PostgreSQL 17
--  說明     : 所有資料庫存取皆透過以下 SP / Function 進行
--             1. fn_get_seating_layout : 讀取各樓層座位 (含佔用員工)
--             2. fn_get_employees       : 讀取員工清單 (供下拉選單)
--             3. sp_apply_seat_assignments : 批次寫入座位異動 (Transaction)
-- =============================================================

-- -------------------------------------------------------------
-- 1. 讀取各樓層座位 (LEFT JOIN 員工，空位則員工欄為 NULL)
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_get_seating_layout()
RETURNS TABLE (
    floor_seat_seq  INTEGER,
    floor_no        INTEGER,
    seat_no         INTEGER,
    emp_id          CHAR(5),
    emp_name        VARCHAR
)
LANGUAGE sql
STABLE
AS $$
    SELECT sc.floor_seat_seq,
           sc.floor_no,
           sc.seat_no,
           e.emp_id,
           e.name
    FROM   seating_chart sc
    LEFT   JOIN employee e ON e.floor_seat_seq = sc.floor_seat_seq
    ORDER  BY sc.floor_no, sc.seat_no;
$$;

-- -------------------------------------------------------------
-- 2. 讀取員工清單 (供前端下拉選單使用)
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_get_employees()
RETURNS TABLE (
    emp_id          CHAR(5),
    name            VARCHAR,
    email           VARCHAR,
    floor_seat_seq  INTEGER
)
LANGUAGE sql
STABLE
AS $$
    SELECT emp_id, name, email, floor_seat_seq
    FROM   employee
    ORDER  BY emp_id;
$$;

-- -------------------------------------------------------------
-- 3. 批次套用座位異動 (Transaction)
--    參數 p_assignments : JSONB 陣列，每筆為
--        { "empId": "12006", "floorSeatSeq": 3 }   -- 指派座位
--        { "empId": "12006", "floorSeatSeq": null } -- 清除座位
--
--    為避免「員工 A 移到員工 B 原座位」造成 UNIQUE 暫時衝突，
--    採兩階段處理：
--      Pass 1 - 先將所有受影響員工的座位清為 NULL
--      Pass 2 - 再依目標座位重新指派
--
--    本程序不含 COMMIT/ROLLBACK，交由呼叫端 (Spring @Transactional)
--    控制交易；任何 RAISE EXCEPTION 都會使整批回滾。
-- -------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_apply_seat_assignments(p_assignments JSONB)
LANGUAGE plpgsql
AS $$
DECLARE
    v_item    JSONB;
    v_emp_id  CHAR(5);
    v_seq     INTEGER;
    v_cnt     INTEGER;
BEGIN
    -- 基本格式檢查
    IF p_assignments IS NULL OR jsonb_typeof(p_assignments) <> 'array' THEN
        RAISE EXCEPTION 'INVALID_PAYLOAD: assignments 必須為 JSON 陣列';
    END IF;

    -- Pass 1 : 清空所有受影響員工的座位
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_assignments)
    LOOP
        v_emp_id := v_item ->> 'empId';

        -- 員編格式檢查 (固定 5 碼數字，防止異常輸入)
        IF v_emp_id IS NULL OR v_emp_id !~ '^[0-9]{5}$' THEN
            RAISE EXCEPTION 'INVALID_EMP_ID: %', v_emp_id;
        END IF;

        SELECT count(*) INTO v_cnt FROM employee WHERE emp_id = v_emp_id;
        IF v_cnt = 0 THEN
            RAISE EXCEPTION 'EMP_NOT_FOUND: %', v_emp_id;
        END IF;

        UPDATE employee SET floor_seat_seq = NULL WHERE emp_id = v_emp_id;
    END LOOP;

    -- Pass 2 : 依目標座位重新指派
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_assignments)
    LOOP
        v_emp_id := v_item ->> 'empId';

        -- floorSeatSeq 為 null 代表清除座位，Pass 1 已處理，略過
        IF (v_item ->> 'floorSeatSeq') IS NULL THEN
            CONTINUE;
        END IF;

        v_seq := (v_item ->> 'floorSeatSeq')::INTEGER;

        -- 座位需存在
        SELECT count(*) INTO v_cnt FROM seating_chart WHERE floor_seat_seq = v_seq;
        IF v_cnt = 0 THEN
            RAISE EXCEPTION 'SEAT_NOT_FOUND: %', v_seq;
        END IF;

        -- 座位不可被其他員工佔用 (Pass 1 後若仍有人佔用，代表批次外的衝突)
        SELECT count(*) INTO v_cnt
        FROM   employee
        WHERE  floor_seat_seq = v_seq AND emp_id <> v_emp_id;
        IF v_cnt > 0 THEN
            RAISE EXCEPTION 'SEAT_OCCUPIED: %', v_seq;
        END IF;

        UPDATE employee SET floor_seat_seq = v_seq WHERE emp_id = v_emp_id;
    END LOOP;
END;
$$;
