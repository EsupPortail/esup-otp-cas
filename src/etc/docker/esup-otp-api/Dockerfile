FROM node:lts-alpine AS builder
RUN apk add --no-cache git
WORKDIR /app
ARG VERSION
RUN git clone --depth 1 https://github.com/EsupPortail/esup-otp-api --branch ${VERSION} .
RUN npm install --legacy-peer-deps

FROM node:lts-alpine
RUN apk add --no-cache bash
WORKDIR /app
COPY --from=builder /app /app

EXPOSE 3000
CMD ["node", "run"]
