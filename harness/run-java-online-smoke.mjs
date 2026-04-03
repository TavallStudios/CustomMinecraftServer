import { runJavaOnlineSmoke } from './harness-lib.mjs'

const version = process.argv[2]
if (!version) {
  throw new Error('Usage: node ./run-java-online-smoke.mjs <version>')
}

await runJavaOnlineSmoke(version)
