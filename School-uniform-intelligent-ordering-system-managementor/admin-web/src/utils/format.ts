export function formatDateTime(value?: string | null) {
  if (!value) {
    return '--'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

export function formatCurrency(value?: number | string | null) {
  const amount = Number(value ?? 0)
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format(Number.isFinite(amount) ? amount : 0)
}

export function splitImageUrls(value?: string | null) {
  if (!value) {
    return []
  }
  const content = value.trim()
  if (!content) {
    return []
  }
  if (content.startsWith('[')) {
    try {
      const parsed = JSON.parse(content)
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item).trim()).filter(Boolean)
      }
    } catch {
      // Ignore JSON parse failure and fallback to delimiter split.
    }
  }
  return content
    .split(/[|,]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function joinImageUrls(values: string[]) {
  return values.map((item) => item.trim()).filter(Boolean).join(',')
}
