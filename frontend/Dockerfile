FROM node:22 AS node

ARG ROCKETCHAT_URL
ENV ROCKETCHAT_URL=$ROCKETCHAT_URL
COPY . /opt/app
WORKDIR /opt/app/frontend
RUN npm install && npm run build

FROM nginx:latest

RUN addgroup --gid 1026 mygroup && adduser --disabled-password --ingroup mygroup --system myuser
RUN mkdir -p /var/cache/nginx && chown -R myuser /var/cache/nginx
RUN touch /var/run/nginx.pid && chown myuser /var/run/nginx.pid

USER myuser

COPY frontend/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=node /opt/app/frontend/dist/rocketchat-archive /usr/share/nginx/html
EXPOSE 80

