import mineflayer from 'mineflayer'
import bedrockProtocol from 'bedrock-protocol'
import minecraftData from 'minecraft-data'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const HARNESS_DIR = path.dirname(fileURLToPath(import.meta.url))
const AUTH_CACHE_ROOT = path.join(HARNESS_DIR, 'auth-cache')

export async function runJavaSmoke(version) {
  const expectedBranch = version.startsWith('1.8') ? 'JAVA_1_8_X' : 'JAVA_1_21_X'
  const username = version.startsWith('1.8') ? 'LegacyHarness' : 'ModernHarness'
  const result = {
    edition: 'JAVA',
    version,
    connected: false,
    branchSelected: false,
    responseReceived: false,
    sessionSignal: version.startsWith('1.8') ? 'LEGACY_LOGIN_DISCONNECT' : 'SETTINGS_PING_PONG',
    sessionSignalObserved: version.startsWith('1.8'),
    reason: ''
  }

  const bot = mineflayer.createBot({
    host: '127.0.0.1',
    port: 25565,
    username,
    version,
    auth: 'offline'
  })
  trackJavaSignals(bot, version, result)

  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error(`Java ${version} timed out`)), 15000)
      let settled = false

      bot.once('login', () => {
        result.connected = true
      })

      bot.once('kicked', reason => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        result.connected = true
        result.responseReceived = true
        result.reason = normalizeReason(reason)
        result.branchSelected = result.reason.includes(expectedBranch)
        resolve()
      })

      bot.once('error', error => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        reject(error)
      })

      bot.once('end', reason => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        if (result.responseReceived) {
          resolve()
        } else {
          reject(new Error(`Java ${version} ended early: ${String(reason)}`))
        }
      })
    })
  } finally {
    bot.quit()
  }

  if (!result.branchSelected) {
    throw new Error(`Java ${version} did not reach expected branch ${expectedBranch}. Reason: ${result.reason}`)
  }
  if (!result.sessionSignalObserved) {
    throw new Error(`Java ${version} did not reach expected signal ${result.sessionSignal}`)
  }

  printSummary(result)
  return result
}

export async function runJavaOnlineSmoke(version, username = process.env.CUSTOM_MINECRAFT_MICROSOFT_USER ?? 'custom-minecraft-server-auth') {
  const expectedMarker = 'ONLINE profile='
  const result = {
    edition: 'JAVA',
    version,
    connected: false,
    branchSelected: false,
    responseReceived: false,
    sessionSignal: version.startsWith('1.8') ? 'LEGACY_LOGIN_DISCONNECT' : 'SETTINGS_PING_PONG',
    sessionSignalObserved: version.startsWith('1.8'),
    reason: ''
  }

  const bot = mineflayer.createBot({
    host: '127.0.0.1',
    port: 25565,
    username,
    version,
    auth: 'microsoft',
    profilesFolder: path.join(AUTH_CACHE_ROOT, 'java'),
    onMsaCode: logMsaCode
  })
  trackJavaSignals(bot, version, result)

  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error(`Java ${version} online auth timed out`)), 180000)
      let settled = false

      bot.once('connect', () => {
        result.connected = true
      })

      bot.once('login', () => {
      })

      bot.once('kicked', reason => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        result.responseReceived = true
        result.reason = normalizeReason(reason)
        result.branchSelected = result.reason.includes(expectedMarker)
        resolve()
      })

      bot.once('error', error => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        reject(error)
      })

      bot.once('end', reason => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        if (result.responseReceived) {
          resolve()
        } else {
          reject(new Error(`Java ${version} online auth ended early: ${String(reason)}`))
        }
      })
    })
  } finally {
    bot.quit()
  }

  if (!result.branchSelected) {
    throw new Error(`Java ${version} online auth did not reach expected marker ${expectedMarker}. Reason: ${result.reason}`)
  }
  if (!result.sessionSignalObserved) {
    throw new Error(`Java ${version} online auth did not reach expected signal ${result.sessionSignal}`)
  }

  printSummary(result)
  return result
}

export async function runBedrockSmoke(version = bedrockProtocolVersion().minecraftVersion) {
  const expectedProtocol = bedrockProtocolVersion(version).version
  const result = {
    edition: 'BEDROCK',
    version,
    connected: false,
    branchSelected: false,
    responseReceived: false,
    sessionSignal: 'START_GAME_BOOTSTRAP',
    sessionSignalObserved: false,
    reason: ''
  }

  const client = bedrockProtocol.createClient({
    host: '127.0.0.1',
    port: 19132,
    username: 'BedrockHarness',
    version,
    offline: true,
    raknetBackend: 'jsp-raknet',
    useRaknetWorkers: false
  })
  client.once('start_game', () => {
    result.sessionSignalObserved = true
  })

  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error(`Bedrock ${version} timed out`)), 15000)
      let settled = false

      client.once('loggingIn', () => {
        result.connected = true
      })

      client.once('kick', packet => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        result.responseReceived = true
        result.reason = typeof packet?.message === 'string' ? packet.message : JSON.stringify(packet)
        result.branchSelected = result.reason.includes(`protocol=${expectedProtocol}`)
        resolve()
      })

      client.once('error', error => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        reject(error)
      })

      client.once('close', () => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        if (result.responseReceived) {
          resolve()
        } else {
          reject(new Error(`Bedrock ${version} closed without deterministic disconnect`))
        }
      })
    })
  } finally {
    client.close()
  }

  if (!result.branchSelected) {
    throw new Error(`Bedrock ${version} did not return expected protocol ${expectedProtocol}. Reason: ${result.reason}`)
  }
  if (!result.sessionSignalObserved) {
    throw new Error(`Bedrock ${version} did not reach expected signal ${result.sessionSignal}`)
  }

  printSummary(result)
  return result
}

export async function runBedrockOnlineSmoke(
  version = bedrockProtocolVersion().minecraftVersion,
  username = process.env.CUSTOM_MINECRAFT_MICROSOFT_USER ?? 'custom-minecraft-server-auth'
) {
  const result = {
    edition: 'BEDROCK',
    version,
    connected: false,
    branchSelected: false,
    responseReceived: false,
    sessionSignal: 'START_GAME_BOOTSTRAP',
    sessionSignalObserved: false,
    reason: ''
  }

  const client = bedrockProtocol.createClient({
    host: '127.0.0.1',
    port: 19132,
    username,
    version,
    offline: false,
    profilesFolder: path.join(AUTH_CACHE_ROOT, 'bedrock'),
    onMsaCode: logMsaCode,
    raknetBackend: 'jsp-raknet',
    useRaknetWorkers: false
  })
  client.once('start_game', () => {
    result.sessionSignalObserved = true
  })

  try {
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error(`Bedrock ${version} online auth timed out`)), 180000)
      let settled = false

      client.once('loggingIn', () => {
        result.connected = true
      })

      client.once('kick', packet => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        result.responseReceived = true
        result.reason = typeof packet?.message === 'string' ? packet.message : JSON.stringify(packet)
        result.branchSelected = result.reason.includes('ONLINE xuid=')
        resolve()
      })

      client.once('error', error => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        reject(error)
      })

      client.once('close', () => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        if (result.responseReceived) {
          resolve()
        } else {
          reject(new Error(`Bedrock ${version} online auth closed without deterministic disconnect`))
        }
      })
    })
  } finally {
    client.close()
  }

  if (!result.branchSelected) {
    throw new Error(`Bedrock ${version} online auth did not reach expected marker ONLINE xuid=. Reason: ${result.reason}`)
  }
  if (!result.sessionSignalObserved) {
    throw new Error(`Bedrock ${version} online auth did not reach expected signal ${result.sessionSignal}`)
  }

  printSummary(result)
  return result
}

export function bedrockProtocolVersion(explicitVersion) {
  const releases = minecraftData.versions.bedrock.filter(entry => entry.releaseType === 'release')
  if (!explicitVersion) {
    return releases[0]
  }
  const match = releases.find(entry => entry.minecraftVersion === explicitVersion)
  if (!match) {
    throw new Error(`Unsupported Bedrock harness version ${explicitVersion}`)
  }
  return match
}

function normalizeReason(reason) {
  if (typeof reason === 'string') {
    return reason
  }
  try {
    return JSON.stringify(reason)
  } catch {
    return String(reason)
  }
}

function printSummary(result) {
  console.log([
    `edition=${result.edition}`,
    `version=${result.version}`,
    `connected=${result.connected}`,
    `branchSelected=${result.branchSelected}`,
    `sessionSignal=${result.sessionSignal}`,
    `sessionSignalObserved=${result.sessionSignalObserved}`,
    `responseReceived=${result.responseReceived}`,
    `reason=${result.reason}`
  ].join(' '))
}

function trackJavaSignals(bot, version, result) {
  if (version.startsWith('1.8') || !bot?._client) {
    return
  }

  bot._client.once('keep_alive', () => {
    try {
      bot._client.write('settings', {
        locale: 'en_US',
        viewDistance: 12,
        chatFlags: 0,
        chatColors: true,
        skinParts: 0x7f,
        mainHand: 1,
        enableTextFiltering: false,
        enableServerListing: true,
        particleStatus: 0
      })
    } catch {
    }
  })
  bot._client.once('ping', () => {
    result.sessionSignalObserved = true
  })
}

function logMsaCode(code) {
  if (!code) return
  if (code.message) {
    console.log(code.message)
    return
  }
  if (code.verification_uri && code.user_code) {
    console.log(`To sign in, open ${code.verification_uri} and enter code ${code.user_code}`)
  }
}
