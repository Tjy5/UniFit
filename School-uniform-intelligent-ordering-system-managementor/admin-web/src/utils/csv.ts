function escapeCsvCell(value: string | number | null | undefined) {
  const text = String(value ?? '')
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

export function downloadCsv(filename: string, header: Array<string | number>, rows: Array<Array<string | number | null | undefined>>) {
  const content = [
    header.map(escapeCsvCell).join(','),
    ...rows.map((row) => row.map(escapeCsvCell).join(',')),
  ].join('\r\n')

  const blob = new Blob(['\uFEFF', content], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

export function formatCsvPercent(value?: number, digits = 1) {
  return `${((value ?? 0) * 100).toFixed(digits)}%`
}

export function formatCsvDateTime(value?: string | Date | null) {
  if (!value) {
    return ''
  }
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-') + ' ' + [
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
    String(date.getSeconds()).padStart(2, '0'),
  ].join(':')
}
