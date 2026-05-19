import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const projectRoot = join(scriptDir, '..')

function read(relativePath) {
  return readFileSync(join(projectRoot, relativePath), 'utf8')
}

const routerSource = read('src/router/index.js')
const mallSource = read('src/views/MallHome.vue')
const wishlistSource = read('src/views/MyWishlist.vue')
const detailSource = read('src/views/ProductDetail.vue')

const issues = []

if (!routerSource.includes("path: '/uniforms/:uniformId'") || !routerSource.includes("name: 'ProductDetail'")) {
  issues.push('src/router/index.js: missing ProductDetail route registration')
}

if (!mallSource.includes("name: 'ProductDetail'")) {
  issues.push('src/views/MallHome.vue: missing Vue Router navigation to ProductDetail')
}

if (!wishlistSource.includes("name: 'ProductDetail'")) {
  issues.push('src/views/MyWishlist.vue: missing Vue Router navigation to ProductDetail')
}

if (mallSource.includes('window.location') || wishlistSource.includes('window.location')) {
  issues.push('MallHome/MyWishlist: navigation must not use window.location')
}

if (!detailSource.includes('RECOMMENDATION_SOURCES.PRODUCT_DETAIL')) {
  issues.push('src/views/ProductDetail.vue: missing PRODUCT_DETAIL recommendation source usage')
}

if (detailSource.includes("'product-detail'") || detailSource.includes('"product-detail"')) {
  issues.push('src/views/ProductDetail.vue: should not use product-detail string literal directly')
}

if (issues.length > 0) {
  console.error('Recommendation entry-point verification failed:')
  for (const issue of issues) {
    console.error(`- ${issue}`)
  }
  process.exit(1)
}

console.log('Recommendation entry-point verification passed.')
