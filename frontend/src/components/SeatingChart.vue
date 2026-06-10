<script setup>
import { ref, computed, onMounted } from 'vue'
import { getEmployees, getSeats, applyAssignments } from '../api/seating'

// ---- 狀態 ----
const employees = ref([])      // 員工清單 (下拉選單)
const seats = ref([])          // 座位佈局 (工作中副本)
const selectedEmpId = ref('')  // 目前選擇的員工
const loading = ref(false)
const message = ref(null)      // { type: 'success' | 'error' | 'info', text }

// ---- 載入資料 ----
async function loadData() {
  loading.value = true
  message.value = null
  try {
    const [empRes, seatRes] = await Promise.all([getEmployees(), getSeats()])
    employees.value = empRes.data.data
    // 同時保存原始狀態 (originalEmpId) 以便比對異動
    seats.value = seatRes.data.data.map((s) => ({
      ...s,
      originalEmpId: s.empId,
      originalEmpName: s.empName
    }))
  } catch (e) {
    message.value = { type: 'error', text: '載入資料失敗，請確認後端服務是否啟動' }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

// ---- 衍生資料 ----
// 依樓層分組
const floors = computed(() => {
  const map = new Map()
  for (const seat of seats.value) {
    if (!map.has(seat.floorNo)) map.set(seat.floorNo, [])
    map.get(seat.floorNo).push(seat)
  }
  return Array.from(map.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([floorNo, list]) => ({
      floorNo,
      seats: list.sort((a, b) => a.seatNo - b.seatNo)
    }))
})

// 是否有未送出的異動
const hasChanges = computed(() => seats.value.some((s) => s.empId !== s.originalEmpId))

// 座位狀態：empty(空位) / occupied(已佔用) / pending(請選擇/新指派)
function seatStatus(seat) {
  if (!seat.empId) return 'empty'
  if (seat.empId !== seat.originalEmpId) return 'pending'
  return 'occupied'
}

// ---- 互動 ----
function onSeatClick(seat) {
  message.value = null

  if (seat.empId) {
    // 已佔用 (含新指派) -> 清除為空位
    seat.empId = null
    seat.empName = null
    return
  }

  // 空位 -> 指派目前選擇的員工
  if (!selectedEmpId.value) {
    message.value = { type: 'info', text: '請先從下拉選單選擇員工，再點選空位' }
    return
  }

  const emp = employees.value.find((e) => e.empId === selectedEmpId.value)
  if (!emp) return

  // 每位員工只能佔用一個座位：若該員工已在其他座位 (工作中)，先清空
  const previous = seats.value.find((s) => s.empId === emp.empId)
  if (previous) {
    previous.empId = null
    previous.empName = null
  }

  seat.empId = emp.empId
  seat.empName = emp.name
  selectedEmpId.value = '' // 指派後清除選擇，避免誤植
}

// 員工目前 (工作中) 是否已就座，下拉選單顯示提示用
function empSeatLabel(emp) {
  const seat = seats.value.find((s) => s.empId === emp.empId)
  if (!seat) return '尚未就座'
  return `${seat.floorNo}樓-座位${seat.seatNo}`
}

// 計算原始 vs 工作中的差異，組成送出的 assignments
function buildAssignments() {
  const original = {}
  const working = {}
  for (const s of seats.value) {
    if (s.originalEmpId) original[s.originalEmpId] = s.floorSeatSeq
    if (s.empId) working[s.empId] = s.floorSeatSeq
  }
  const ids = new Set([...Object.keys(original), ...Object.keys(working)])
  const result = []
  for (const id of ids) {
    const o = original[id] != null ? original[id] : null
    const w = working[id] != null ? working[id] : null
    if (o !== w) result.push({ empId: id, floorSeatSeq: w })
  }
  return result
}

async function onSubmit() {
  const assignments = buildAssignments()
  if (assignments.length === 0) {
    message.value = { type: 'info', text: '尚無座位異動' }
    return
  }
  loading.value = true
  message.value = null
  try {
    await applyAssignments(assignments)
    message.value = { type: 'success', text: '座位更新成功' }
    await loadData() // 重新載入，重置工作中副本
  } catch (e) {
    const text = e?.response?.data?.message || '座位更新失敗'
    message.value = { type: 'error', text }
  } finally {
    loading.value = false
  }
}

function onReset() {
  message.value = null
  selectedEmpId.value = ''
  seats.value = seats.value.map((s) => ({
    ...s,
    empId: s.originalEmpId,
    empName: s.originalEmpName
  }))
}
</script>

<template>
  <div class="seating">
    <!-- 操作列 -->
    <div class="toolbar">
      <label class="field">
        <span>選擇員工：</span>
        <select v-model="selectedEmpId" :disabled="loading">
          <option value="">-- 請選擇員工 --</option>
          <option v-for="emp in employees" :key="emp.empId" :value="emp.empId">
            {{ emp.empId }} {{ emp.name }}（{{ empSeatLabel(emp) }}）
          </option>
        </select>
      </label>
      <div class="actions">
        <button class="btn btn--ghost" :disabled="loading || !hasChanges" @click="onReset">
          取消變更
        </button>
        <button class="btn btn--primary" :disabled="loading || !hasChanges" @click="onSubmit">
          送出
        </button>
      </div>
    </div>

    <!-- 訊息 -->
    <p v-if="message" :class="['message', 'message--' + message.type]">{{ message.text }}</p>

    <!-- 座位圖 -->
    <div v-if="loading && seats.length === 0" class="placeholder">載入中…</div>

    <div v-else class="chart">
      <div v-for="floor in floors" :key="floor.floorNo" class="floor-row">
        <button
          v-for="seat in floor.seats"
          :key="seat.floorSeatSeq"
          type="button"
          :class="['seat', 'seat--' + seatStatus(seat)]"
          :title="seatStatus(seat) === 'empty' ? '空位，點選以指派員工' : '已佔用，點選以清除'"
          @click="onSeatClick(seat)"
        >
          <span class="seat__label">{{ seat.floorNo }}樓: 座位{{ seat.seatNo }}</span>
          <span v-if="seat.empId" class="seat__emp">[員編:{{ seat.empId }}]</span>
        </button>
      </div>
    </div>

    <!-- 圖例 -->
    <div class="legend">
      <span class="legend__item"><i class="swatch swatch--empty"></i>空位</span>
      <span class="legend__item"><i class="swatch swatch--occupied"></i>已佔用</span>
      <span class="legend__item"><i class="swatch swatch--pending"></i>請選擇</span>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.field {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.field select {
  padding: 8px 10px;
  font-size: 14px;
  border: 1px solid #bbb;
  border-radius: 4px;
  min-width: 260px;
}
.actions {
  display: flex;
  gap: 8px;
}
.btn {
  padding: 8px 20px;
  font-size: 14px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--primary {
  background: #1565c0;
  color: #fff;
}
.btn--ghost {
  background: #e0e0e0;
  color: #333;
}

.message {
  padding: 10px 14px;
  border-radius: 4px;
  font-size: 14px;
  margin: 0 0 16px;
}
.message--success { background: #e6f4ea; color: #1b7a3d; border: 1px solid #b6dfc3; }
.message--error   { background: #fdecea; color: #b3261e; border: 1px solid #f5c2bd; }
.message--info    { background: #e8f0fe; color: #1a56c4; border: 1px solid #bcd2f7; }

.placeholder {
  padding: 40px;
  text-align: center;
  color: #888;
}

.chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.floor-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.seat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 64px;
  padding: 10px;
  border: 1px solid #cfd4d9;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.12s ease;
}
.seat:hover {
  filter: brightness(0.95);
}
.seat__emp {
  font-size: 12px;
  font-weight: 700;
}
/* 空位：灰 */
.seat--empty {
  background: #eef1f4;
  color: #333;
}
/* 已佔用：紅 */
.seat--occupied {
  background: #dc3545;
  color: #fff;
  border-color: #c62f3e;
}
/* 請選擇 (新指派)：綠 */
.seat--pending {
  background: #8ce99a;
  color: #14532d;
  border-color: #69c47b;
}

.legend {
  display: flex;
  gap: 20px;
  margin-top: 20px;
  font-size: 14px;
  color: #444;
}
.legend__item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.swatch {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 3px;
  border: 1px solid #cfd4d9;
}
.swatch--empty { background: #eef1f4; }
.swatch--occupied { background: #dc3545; }
.swatch--pending { background: #8ce99a; }
</style>
