import { runBedrockOnlineSmoke } from './harness-lib.mjs'

const version = process.argv[2]

await runBedrockOnlineSmoke(version)
