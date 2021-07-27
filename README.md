# rocketchat-archive

Fast searchable online archive for instances of [Rocket.Chat](https://rocket.chat/).

## Installation and deployment

Clone the Git repository to your local machine or server.
Then build frontend, backend, the docker images.
Finally, fire up everything

### Requirements

* Git
* Docker
* Docker-Compose
* Node.js (recently enough; Node 16.5.0 was used for development)

### Build frontend

From the `frontend` directory, run

```
npm install
npm run build
```

Remember to add the `--base-href` switch in case the application will not be deployed
on top level of your domain, e.g.

```
npm run build -- --base-href="/archive/"
```

### Build backend

From the `backend` directory, run

```
./gradlew distTar
```

### Build docker images

From the root directory of the repository, run

```
docker-compose build
```

### Run

Finally, you are ready to start everything up:

```
docker-compose up
```

The frontend will listen on port `42773` locally, so you may want to point your browser to http://localhost:42773/.

### Things to know

* The backend connects to a host `mongo` in the network `rocketchat_default`.
  This is intended to be used with a Docker-based deployment of Rocket.Chat
  (e.g., [https://docs.rocket.chat/installing-and-updating/docker-containers/systemd](https://docs.rocket.chat/installing-and-updating/docker-containers/systemd)).
  You can change this configuration in `docker-compose.yml`. 
* You may want to configure your web server to be a reverse proxy that forwards certain paths to port `42773`.
  E.g., when using the Apache configuration from
  [https://docs.rocket.chat/installing-and-updating/manual-installation/configuring-ssl-reverse-proxy](https://docs.rocket.chat/installing-and-updating/docker-containers/systemd),
  you can change the configuration of the modules `mod-rewrite` and `mod-proxy` to:
```
RewriteEngine On
RewriteCond %{HTTP:CONNECTION} Upgrade [NC]
RewriteCond %{HTTP:Upgrade}    =websocket [NC]
RewriteRule /(.*)              ws://localhost:3000/$1 [P,L]
RewriteCond %{HTTP:Upgrade}    !=websocket [NC]
RewriteCond %{REQUEST_URI}     !maintenance.html
RewriteCond %{REQUEST_URI}     !/archive
RewriteRule /(.*)              http://localhost:3000/$1 [P,L]

RewriteRule /archive/(.*)      http://localhost:42773/$1 [P,L]

ProxyPassReverse /archive/     http://localhost:42773/
ProxyPassReverse /             http://localhost:3000/
```
* The frontend listens to port `42773`. You can change this port in `docker-compose.yml`.
* You may want to create a Systemd unit to ensure the application is started automatically on boot.
  Take this snippet as a template:
```
[Unit]
Description=Rocketchat archive
Requires=mongo.service
After=mongo.service

[Service]
User=paulchen
Type=oneshot
RemainAfterExit=true
WorkingDirectory=/opt/rocketchat-archive/
ExecStart=/usr/local/bin/docker-compose up -d --remove-orphans
ExecStop=/usr/local/bin/docker-compose down

[Install]
WantedBy=multi-user.target
```
* This application does not involve any authentication.
  Therefore, without taking any additional measures, all messages in all channels would be exposed to the public.
  To apply basic HTTP authentication, you can extend the above proxy configuration by the following lines:
```
<Location /archive>
  AuthUserFile /etc/apache2/htpasswd-archive
  AuthName "You shall not pass!"
  AuthType Basic
  require valid-user
</Location>
```
  Don't forget to create the file `/etc/apache2/htpasswd-archive` using

```
htpasswd -c /etc/apache2/htpasswd-archive <username>
```
