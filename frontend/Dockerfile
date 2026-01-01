FROM node:24 AS node

COPY . /opt/app
WORKDIR /opt/app/frontend
RUN npm install && npm run build

FROM nginx:latest

RUN apt-get update && \
    apt-get install --no-install-recommends --no-install-suggests -y adduser && \
    rm -rf /var/lib/apt/lists/* && \
    addgroup --gid 1026 mygroup && \
    adduser --disabled-password --ingroup mygroup --system myuser && \
    mkdir -p /var/cache/nginx && \
    chown -R myuser /var/cache/nginx && \
    touch /var/run/nginx.pid && \
    chown myuser /var/run/nginx.pid

USER myuser

COPY frontend/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=node /opt/app/frontend/dist/rocketchat-archive /usr/share/nginx/html
EXPOSE 80

