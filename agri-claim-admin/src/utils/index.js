export function formatDateTime(date, fmt = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  const d = new Date(date)
  const map = {
    'YYYY': d.getFullYear(),
    'MM': String(d.getMonth() + 1).padStart(2, '0'),
    'DD': String(d.getDate()).padStart(2, '0'),
    'HH': String(d.getHours()).padStart(2, '0'),
    'mm': String(d.getMinutes()).padStart(2, '0'),
    'ss': String(d.getSeconds()).padStart(2, '0')
  }
  return fmt.replace(/YYYY|MM|DD|HH|mm|ss/g, (k) => map[k])
}

export function formatDate(date) {
  return formatDateTime(date, 'YYYY-MM-DD')
}

export function formatAmount(value, digits = 2) {
  if (value === null || value === undefined || value === '') return '0.00'
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  })
}

export function formatArea(value, unit = '亩') {
  if (value === null || value === undefined || value === '') return `0 ${unit}`
  return `${Number(value).toFixed(2)} ${unit}`
}

export function formatPercent(value, digits = 2) {
  if (value === null || value === undefined || value === '') return '0.00%'
  return `${Number(value).toFixed(digits)}%`
}

export function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(2)} ${units[i]}`
}

export function toChineseAmount(num) {
  if (num === null || num === undefined || num === '') return '零元整'
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const units1 = ['', '拾', '佰', '仟']
  const units2 = ['', '万', '亿', '兆']
  let s = Number(num).toFixed(2)
  let [intPart, decPart] = s.split('.')
  if (parseInt(intPart) === 0) {
    if (decPart === '00') return '零元整'
    return `零元${centsText(decPart)}`
  }
  let result = ''
  const len = intPart.length
  for (let i = 0; i < len; i++) {
    const d = parseInt(intPart.charAt(i))
    const pos = len - 1 - i
    const unitIdx = Math.floor(pos / 4)
    const subIdx = pos % 4
    if (d !== 0) {
      result += digits[d] + units1[subIdx]
    } else if (subIdx === 0 && unitIdx < units2.length) {
      result += units2[unitIdx]
    }
    if (subIdx === 0 && unitIdx > 0 && unitIdx < units2.length && !result.endsWith(units2[unitIdx])) {
      result += units2[unitIdx]
    }
  }
  result += '元'
  if (decPart === '00') {
    result += '整'
  } else {
    result += centsText(decPart)
  }
  return result.replace(/零+/g, '零').replace('零万', '万').replace('零亿', '亿').replace('零元', '元')
}

function centsText(s) {
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  let r = ''
  if (s.charAt(0) !== '0') r += digits[parseInt(s.charAt(0))] + '角'
  if (s.charAt(1) !== '0') r += digits[parseInt(s.charAt(1))] + '分'
  return r
}

export function disasterTypeText(type) {
  const map = {
    FLOOD: '淹水灾害',
    LODGE: '倒伏灾害',
    WITHER: '枯黄灾害',
    DROUGHT: '干旱灾害',
    HAIL: '冰雹灾害'
  }
  return map[type] || type || '-'
}

export function disasterLevelText(level) {
  const map = { LIGHT: '轻度', MODERATE: '中度', SEVERE: '重度' }
  return map[level] || level || '-'
}

export function disasterLevelTag(level) {
  const map = {
    LIGHT: { type: 'success', text: '轻度' },
    MODERATE: { type: 'warning', text: '中度' },
    SEVERE: { type: 'danger', text: '重度' }
  }
  return map[level] || { type: 'info', text: level || '-' }
}

export function imageStatusText(status) {
  const map = {
    UPLOADED: '已上传',
    PREPROCESSING: '预处理中',
    PREPROCESSED: '预处理完成',
    PROCESSING: 'AI处理中',
    COMPLETED: '处理完成',
    FAILED: '处理失败'
  }
  return map[status] || status || '-'
}

export function assessStatusText(status) {
  const map = {
    DRAFT: '草稿',
    PROCESSING: '处理中',
    PENDING: '待审核',
    AUDIT: '审核中',
    APPROVED: '审核通过',
    REJECTED: '审核驳回',
    PAID: '已赔付'
  }
  return map[status] || status || '-'
}

export function assessStatusTag(status) {
  const map = {
    DRAFT: { type: 'info', text: '草稿' },
    PROCESSING: { type: 'warning', text: '处理中' },
    PENDING: { type: 'warning', text: '待审核' },
    AUDIT: { type: 'primary', text: '审核中' },
    APPROVED: { type: 'success', text: '审核通过' },
    REJECTED: { type: 'danger', text: '审核驳回' },
    PAID: { type: 'success', text: '已赔付' }
  }
  return map[status] || { type: 'info', text: status || '-' }
}

export function imageTypeText(type) {
  const map = { BEFORE: '灾前影像', AFTER: '灾后影像', ORTHO: '正射影像', DEM: 'DEM高程' }
  return map[type] || type || '-'
}

export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj.getTime())
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  const result = {}
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      result[key] = deepClone(obj[key])
    }
  }
  return result
}

export function debounce(fn, wait = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), wait)
  }
}

export function throttle(fn, wait = 300) {
  let last = 0
  return function (...args) {
    const now = Date.now()
    if (now - last >= wait) {
      last = now
      fn.apply(this, args)
    }
  }
}

export function generateMissionNo(prefix = 'AM') {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const r = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
  return `${prefix}${y}${m}${d}${r}`
}

export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(new Blob([blob]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
