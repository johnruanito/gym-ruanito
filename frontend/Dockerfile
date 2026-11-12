# syntax=docker/dockerfile:1.7

# ---- build: gera os arquivos estáticos do React ----
FROM node:25-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# ---- runtime: só o nginx servindo os arquivos ----
# alpine-slim: nginx essencial, sem curl, módulos XSLT/imagem e libs que não usamos
FROM nginx:1.31-alpine-slim
# aplica as correções de segurança publicadas pelo Alpine depois que a imagem base foi gerada
RUN apk upgrade --no-cache
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80