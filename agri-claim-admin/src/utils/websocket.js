const WS_RECONNECT_INTERVAL = 3000
const WS_MAX_RECONNECT = 10
const WS_HEARTBEAT_INTERVAL = 30000

class AssessWebSocket {
  constructor() {
    this.ws = null
    this.url = null
    this.listeners = new Map()
    this.reconnectCount = 0
    this.manualClose = false
    this.heartbeatTimer = null
  }

  connect(userId) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return this.ws
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    this.url = `${protocol}//${host}/ws/assess?userId=${userId || localStorage.getItem('userId') || ''}`
    this.manualClose = false
    this._connect()
    return this
  }

  _connect() {
    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = (e) => {
        this.reconnectCount = 0
        this._startHeartbeat()
        this._emit('open', e)
      }

      this.ws.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data)
          this._emit('message', data)
          this._emit(data.type, data)
        } catch (err) {
          this._emit('message', e.data)
        }
      }

      this.ws.onerror = (e) => {
        this._emit('error', e)
      }

      this.ws.onclose = (e) => {
        this._stopHeartbeat()
        this._emit('close', e)
        if (!this.manualClose && this.reconnectCount < WS_MAX_RECONNECT) {
          this.reconnectCount++
          setTimeout(() => this._connect(), WS_RECONNECT_INTERVAL)
        }
      }
    } catch (e) {
      console.error('WebSocket连接失败', e)
    }
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const payload = typeof data === 'string' ? data : JSON.stringify(data)
      this.ws.send(payload)
      return true
    }
    return false
  }

  subscribe(userId) {
    return this.send({ type: 'SUBSCRIBE', userId })
  }

  on(type, callback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type).add(callback)
    return () => this.off(type, callback)
  }

  off(type, callback) {
    const listeners = this.listeners.get(type)
    if (listeners) {
      listeners.delete(callback)
    }
  }

  _emit(type, data) {
    const listeners = this.listeners.get(type)
    if (listeners) {
      listeners.forEach(fn => {
        try { fn(data) } catch (e) { console.error(e) }
      })
    }
  }

  _startHeartbeat() {
    this._stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'PING', timestamp: Date.now() })
    }, WS_HEARTBEAT_INTERVAL)
  }

  _stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  close() {
    this.manualClose = true
    this._stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.listeners.clear()
  }

  isOpen() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

const wsInstance = new AssessWebSocket()

export default wsInstance
export { AssessWebSocket }
