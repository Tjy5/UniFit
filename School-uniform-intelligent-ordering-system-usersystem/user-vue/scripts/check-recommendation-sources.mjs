import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const projectRoot = join(scriptDir, '..')

const targetFiles = [
  'src/composables/useSizeRecommendation.js',
  'src/views/MallHome.vue',
  'src/views/MyWishlist.vue',
  'src/views/MyOrders.vue',
  'src/views/ProductDetail.vue',
]

const forbiddenLiterals = [
  "'mall-home-dialog'",
  '"mall-home-dialog"',
  "'wishlist'",
  '"wishlist"',
  "'order-feedback'",
  '"order-feedback"',
  "'product-detail'",
  '"product-detail"',
]

const issues = []

for (const relativePath of targetFiles) {
  const absolutePath = join(projectRoot, relativePath)
  const source = readFileSync(absolutePath, 'utf8')

  if (!source.includes('RECOMMENDATION_SOURCES')) {
    issues.push(`${relativePath}: missing RECOMMENDATION_SOURCES import`)
  }

  const literalMatches = forbiddenLiterals.filter((literal) => source.includes(literal))
  if (literalMatches.length > 0) {
    issues.push(`${relativePath}: contains forbidden source literal(s) ${literalMatches.join(', ')}`)
  }
}

if (issues.length > 0) {
  console.error('Recommendation source lint failed:')
  for (const issue of issues) {
    console.error(`- ${issue}`)
  }
  process.exit(1)
}

console.log('Recommendation source lint passed.')
