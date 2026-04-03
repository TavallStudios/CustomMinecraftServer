import { runJavaSmoke } from './harness-lib.mjs'

const version = process.argv[2]
if (!version) {
  throw new Error('Usage: node ./run-java-smoke.mjs <version>')
}

await runJavaSmoke(version)
