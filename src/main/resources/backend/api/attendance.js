// 考勤管理 API
const attendanceWeekSummary = () => $axios({ url: '/api/attendance/week-summary', method: 'get' })
const attendanceCalendar = (params) => $axios({ url: '/api/attendance/calendar', method: 'get', params })
const attendanceToday = () => $axios({ url: '/api/attendance/today', method: 'get' })
const attendanceAbnormal = (params) => $axios({ url: '/api/attendance/abnormal', method: 'get', params })

// 排班管理 API
const scheduleMonthly = (params) => $axios({ url: '/api/schedule/monthly', method: 'get', params })
const scheduleToday = () => $axios({ url: '/api/schedule/today', method: 'get' })
const scheduleSave = (data) => $axios({ url: '/api/schedule/save', method: 'post', data })
const scheduleList = (params) => $axios({ url: '/api/schedule/list', method: 'get', params })
const scheduleDelete = (id) => $axios({ url: '/api/schedule/' + id, method: 'delete' })
