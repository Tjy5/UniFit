import imageFallback from '@/assets/image_placeholder.png'

const DEFAULT_DEV_ASSET_BASE_URL = 'http://127.0.0.1:9091'

function normalizeInput(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function ensureLeadingSlash(path) {
  return path.startsWith('/') ? path : `/${path}`
}

function isAbsoluteUrl(path) {
  return /^(https?:)?\/\//i.test(path) || /^data:/i.test(path) || /^blob:/i.test(path)
}

function getAssetBaseUrl() {
  const configured = normalizeInput(import.meta.env.VUE_APP_ASSET_BASE_URL || import.meta.env.VITE_ASSET_BASE_URL)
  if (configured) {
    return configured.replace(/\/+$/, '')
  }

  if (!import.meta.env.PROD) {
    return DEFAULT_DEV_ASSET_BASE_URL
  }

  return ''
}

function isManagedUploadPath(path) {
  return (
    path === '/profile/upload' ||
    path.startsWith('/profile/upload/') ||
    path === '/upload' ||
    path.startsWith('/upload/')
  )
}

export function getImageFallback() {
  return imageFallback
}

export function resolveImageUrl(value) {
  const rawPath = normalizeInput(value)
  if (!rawPath) {
    return imageFallback
  }

  if (isAbsoluteUrl(rawPath)) {
    return rawPath
  }

  const normalizedPath = ensureLeadingSlash(rawPath.replace(/\\/g, '/'))

  if (normalizedPath.startsWith('/api/')) {
    return normalizedPath
  }

  if (isManagedUploadPath(normalizedPath)) {
    const assetBaseUrl = getAssetBaseUrl()
    return assetBaseUrl ? `${assetBaseUrl}${normalizedPath}` : normalizedPath
  }

  return normalizedPath
}

export function resolveImageList(value) {
  if (Array.isArray(value)) {
    return value
      .map((item) => normalizeInput(item))
      .filter(Boolean)
      .map((item) => resolveImageUrl(item))
  }

  const rawValue = normalizeInput(value)
  if (!rawValue) {
    return []
  }

  return rawValue
    .split(',')
    .map((item) => normalizeInput(item))
    .filter(Boolean)
    .map((item) => resolveImageUrl(item))
}
