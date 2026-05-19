import { readFileSync, readdirSync } from 'node:fs'
import { join, relative } from 'node:path'

const projectRoot = join(import.meta.dirname, '..')
const sourceRoot = join(projectRoot, 'src')
const deprecatedPatterns = [
  { label: '::v-deep combinator', pattern: /::v-deep\s+[^{,]+/ },
  { label: '>>> deep selector', pattern: />>>/ },
  { label: '/deep/ selector', pattern: /\/deep\// },
]

function walk(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? walk(path) : [path]
  })
}

const issues = []
for (const filePath of walk(sourceRoot)) {
  if (!/\.(vue|css|scss|sass|less|js|ts)$/.test(filePath)) {
    continue
  }
  const source = readFileSync(filePath, 'utf8')
  for (const { label, pattern } of deprecatedPatterns) {
    if (pattern.test(source)) {
      issues.push(`${relative(projectRoot, filePath)}: deprecated ${label}`)
    }
  }
}

if (issues.length > 0) {
  console.error('Deprecated Vue deep selector syntax found:')
  for (const issue of issues) {
    console.error(`- ${issue}`)
  }
  console.error('Use Vue-supported :deep(...) syntax instead.')
  process.exit(1)
}

console.log('Deep selector guard passed.')
