import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(() => {
  const apiProxy: ProxyOptions = {
    target: 'http://localhost:18080',
    changeOrigin: true,
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
    },
  }

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    // 局域网共享：其他电脑用本机 IP 打开页面时，浏览器里的 localhost 指向的是
    // 访问者自己而非本机。让前端走同源相对路径 /api（配合 VITE_GATEWAY_URL 置空），
    // 由本机的 Vite 代理转发到网关，即可避免跨机 localhost 解析问题。
    // Vite→网关这一跳是本机服务端调用，剥掉浏览器带来的 Origin，网关便视其为
    // 非跨域请求直接放行，无需把每台机器的 IP 加进网关 CORS 白名单。
    server: {
      host: '0.0.0.0',
      proxy: { '/api': apiProxy },
    },
    preview: {
      host: '0.0.0.0',
      proxy: { '/api': apiProxy },
    },
    test: {
      environment: 'jsdom',
      globals: true,
      // 逐用例清理 sessionStorage 里的认证态，避免跨用例泄漏。
      setupFiles: ['./src/tests/setup.ts'],
      // 单测始终跑演示模式：开发者本地 .env.local 可能设 VITE_API_MODE=real，
      // 不能让它把契约单测导向真实网关。
      env: { VITE_API_MODE: 'demo' },
      testTimeout: 30000,
    },

  }
})
