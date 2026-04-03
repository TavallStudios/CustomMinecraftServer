import { runJavaSmoke, runBedrockSmoke } from './harness-lib.mjs'

await runJavaSmoke('1.8.8')
await runJavaSmoke('1.21.4')
await runBedrockSmoke()
