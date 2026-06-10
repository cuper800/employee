import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/** 取得員工清單 (供下拉選單) */
export function getEmployees() {
  return api.get('/employees')
}

/** 取得各樓層座位佈局 */
export function getSeats() {
  return api.get('/seats')
}

/**
 * 送出座位異動。
 * @param {Array<{empId: string, floorSeatSeq: number|null}>} assignments
 */
export function applyAssignments(assignments) {
  return api.post('/seats/assignments', { assignments })
}
