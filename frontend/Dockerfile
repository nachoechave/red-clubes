FROM node:26.8-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
ENV NGINX_ENVSUBST_FILTER=TRUSTED_PROXY_CIDR
USER root
RUN apk upgrade --no-cache
COPY --chown=101:101 nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build --chown=101:101 /workspace/dist/red-clubes-frontend/browser /usr/share/nginx/html
USER 101:101
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
