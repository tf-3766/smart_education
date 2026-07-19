import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(async () => {
  const frappeui = (await import('frappe-ui/vite')).default

  const apiProxy: ProxyOptions = {
    target: 'http://localhost:18080',
    changeOrigin: true,
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
    },
  }

  return {
    plugins: [
      frappeui({
        frappeProxy: false,
        lucideIcons: true,
        jinjaBootData: false,
        buildConfig: false,
      }),
      vue(),
    ],
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
      // frappe-ui 发布的是未编译源码，挂载页面时需经 vite 管道转换。
      server: { deps: { inline: ['frappe-ui'] } },
      // 演示契约共用同一个内存数据库；测试文件并行会互相 reset，导致随机失败。
      fileParallelism: false,
      // 页面首次挂载要过 vite 转换管道，冷启动可达 15s。
      testTimeout: 30000,
    },
    optimizeDeps: {
      include: ['debug', 'feather-icons', 'socket.io-client', 'socket.io-parser'],
      exclude: ['frappe-ui'],
    },
  }
})
