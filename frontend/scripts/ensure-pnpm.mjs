const userAgent = process.env.npm_config_user_agent ?? ''

if (!userAgent.startsWith('pnpm/11.7.0 ')) {
  console.error('This project requires pnpm 11.7.0. Run: corepack prepare pnpm@11.7.0 --activate')
  process.exit(1)
}