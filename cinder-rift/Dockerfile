# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm,sharing=locked npm ci

COPY index.html vite.config.js ./
COPY src/ src/
COPY public/ public/

# These values are public and embedded by Vite; never pass secrets here.
ARG VITE_API_URL=/api
ARG VITE_USE_MOCK=false
RUN VITE_API_URL="$VITE_API_URL" VITE_USE_MOCK="$VITE_USE_MOCK" npm run build

FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /workspace/dist/ /usr/share/nginx/html/

USER nginx
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=10 \
    CMD ["wget", "--quiet", "--spider", "http://127.0.0.1:8082/healthz"]

# The configuration is complete; skip entrypoint scripts that rewrite /etc/nginx.
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
