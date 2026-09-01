// 考勤管理 API
const attendanceWeekSummary = () => $axios({ url: '/api/attendance/week-summary', method: 'get' })
const attendanceCalendar = (params) => $axios({ url: '/api/attendance/calendar', method: 'get', params })
const attendanceToday = () => $axios({ url: '/api/attendance/today', method: 'get' })
const attendanceAbnormal = (params) => $axios({ url: '/api/attendance/abnormal', method: 'get', params })
const attendanceClockIn = () => $axios({ url: '/api/attendance/clockIn', method: 'post' })
const attendanceClockOut = (params) => $axios({ url: '/api/attendance/clockOut', method: 'post', data: params })

// 排班管理 API
const scheduleMonthly = (params) => $axios({ url: '/api/schedule/monthly', method: 'get', params })
const scheduleToday = () => $axios({ url: '/api/schedule/today', method: 'get' })
const scheduleSave = (data) => $axios({ url: '/api/schedule/save', method: 'post', data })
const scheduleList = (params) => $axios({ url: '/api/schedule/list', method: 'get', params })
const scheduleDelete = (id) => $axios({ url: '/api/schedule/' + id, method: 'delete' })

// 员工选项（用于考勤/排班页面下拉选择）
const employeeOptions = () => $axios({ url: '/employee/options', method: 'get' })
const employeeList = () => $axios({ url: '/employee/list', method: 'get' })
