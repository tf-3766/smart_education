# Build stage: intentionally pins the project engines so host Node/npm versions never affect the image.
FROM node:22.13.0-bookworm-slim AS build

WORKDIR /app
# Avoid Corepack's rotating signing-key dependency in older Node 22 images.
# pnpm is still installed inside the image at the exact project-required version.
RUN npm install --global pnpm@11.7.0 && pnpm --version

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY scripts/ensure-pnpm.mjs ./scripts/ensure-pnpm.mjs
RUN pnpm install --frozen-lockfile

COPY . ./
RUN pnpm run build

# Runtime stage: only Nginx and static assets are shipped to the target machine.
FROM nginx:1.27-alpine

COPY deploy/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
