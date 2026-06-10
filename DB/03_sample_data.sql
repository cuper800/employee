-- =============================================================
--  員工座位系統 - DML (範例資料)
--  Database : PostgreSQL 17
--  說明     : 對應題目範例畫面 (4 樓層，每層 4 個座位)
--             座位序號 (floor_seat_seq) 依樓層、座位順序自動產生 1..16
-- =============================================================

-- 清空既有資料 (TRUNCATE 同時重置序號)
TRUNCATE TABLE employee RESTART IDENTITY CASCADE;
TRUNCATE TABLE seating_chart RESTART IDENTITY CASCADE;

-- -------------------------------------------------------------
-- 樓層座位：1~4 樓，每層 4 個座位
--   seq 1~4   = 1樓 座位1~4
--   seq 5~8   = 2樓 座位1~4
--   seq 9~12  = 3樓 座位1~4
--   seq 13~16 = 4樓 座位1~4
-- -------------------------------------------------------------
INSERT INTO seating_chart (floor_no, seat_no)
SELECT f.floor_no, s.seat_no
FROM   generate_series(1, 4) AS f(floor_no)
CROSS  JOIN generate_series(1, 4) AS s(seat_no)
ORDER  BY f.floor_no, s.seat_no;

-- -------------------------------------------------------------
-- 員工資料 (對應範例畫面已就座者 + 數位未就座者供下拉測試)
--   1樓座位3 (seq 3)  -> 12006
--   2樓座位3 (seq 7)  -> 16142
--   3樓座位1 (seq 9)  -> 13040
--   3樓座位2 (seq 10) -> 17081
--   3樓座位4 (seq 12) -> 11221
--   4樓座位3 (seq 15) -> 16722
-- -------------------------------------------------------------
INSERT INTO employee (emp_id, name, email, floor_seat_seq) VALUES
    ('12006', '王小明', 'ming.wang@esunbank.com',   3),
    ('16142', '林佳蓉', 'jiarong.lin@esunbank.com', 7),
    ('13040', '陳志強', 'zhiqiang.chen@esunbank.com', 9),
    ('17081', '黃淑芬', 'shufen.huang@esunbank.com', 10),
    ('11221', '張家豪', 'jiahao.zhang@esunbank.com', 12),
    ('16722', '李美玲', 'meiling.li@esunbank.com',   15),
    -- 以下為尚未安排座位的員工 (floor_seat_seq = NULL)
    ('10001', '吳孟達', 'mengda.wu@esunbank.com',    NULL),
    ('10002', '蔡英傑', 'yingjie.cai@esunbank.com',  NULL),
    ('20003', '鄭雅婷', 'yating.zheng@esunbank.com', NULL),
    ('20004', '許文彥', 'wenyan.xu@esunbank.com',    NULL);
