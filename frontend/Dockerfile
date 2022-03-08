FROM nginx:latest

RUN addgroup --gid 1026 mygroup && adduser --disabled-password --ingroup mygroup --system myuser
RUN mkdir -p /var/cache/nginx && chown -R myuser /var/cache/nginx
RUN touch /var/run/nginx.pid && chown myuser /var/run/nginx.pid

USER myuser

COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/rocketchat-archive /usr/share/nginx/html
EXPOSE 80

