# rocketchat-archive

Fast searchable online archive for instances of [Rocket.Chat](https://rocket.chat/).

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=paulchen_rocketchat-archive&metric=alert_status)](https://sonarcloud.io/dashboard?id=paulchen_rocketchat-archive)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## Installation and deployment

Clone the Git repository to your local machine or server.
Then build frontend, backend, the docker images.
Finally, fire up everything

### Requirements

* Git
* Docker
* Docker-Compose
* Node.js (recently enough; Node 16.14.2 with npm 8.5.0 was used for development)

### Build frontend

From the `frontend` directory, run

```
npm install
ROCKETCHAT_URL='...' npm run build
```

Set the variable `ROCKETCHAT_URL` to the URL of your Rocket.Chat installation (including the trailing `/`).

Remember to add the `--base-href` switch in case the application will not be deployed
on top level of your domain, e.g.

```
ROCKETCHAT_URL='...' npm run build -- --base-href="/archive/"
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
RAVUSBOT_USERNAME=... RAVUSBOT_PASSWORD=... DATABASE=... FAVORUITE_CHANNELS=... docker-compose up
```

Set the environment variables `RAVUSBOT_USERNAME` and `RAVUSBOT_PASSWORD` to the credentials to access the API of
[RavuAlHemio/rocketbot](https://github.com/RavuAlHemio/rocketbot).
Set `DATABASE` to the name of your MongoDB database (usually `rocketchat`).
Set `FAVOURITE_CHANNELS` to a comma-separated list of channel names (without leading `#`) that should come first
the channel list.
You can add the `TZ` variable and set it to a time zone from the Olsen Database (e.g. `Europe/Vienna`) to get
the timestamps right. 

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
EnvironmentFile=/etc/default/rocketchat-archive

[Install]
WantedBy=multi-user.target
```
* This snippet references the file `/etc/default/rocketchat-archive` which contains the environment variables required for running the archive:
```
RAVUSBOT_USERNAME=...
RAVUSBOT_PASSWORD=...
DATABASE=...
TZ=...
FAVOURITE_CHANNELS=...
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

## Miscellaneous

### Deployment script

To simplify build and deployment of both frontend and backend, there is a deployment script (`misc/deploy.sh`).
To use it, copy `deploy.conf.dist` to `deploy.conf` and configure it accordingly.
The script will perform the following steps:
* Run `git pull`.
* Build the frontend.
* Build the backend.
* Run `docker-compose build`.
* Restart the systemd unit.

If any of the steps fails, the script will abort immediately.

### Icinga check script

This project includes a check script for Icinga (`misc/check_archive.py`).
It requires Python 3 with the Requests library installed. It will check two things:
* The Git revisions of frontend and backend match.
* The backend returns a list of one or more channels.

The script takes its configuration from three environment variables
(`ARCHIVE_BASE_URL`, `ARCHIVE_USERNAME`, `ARCHIVE_PASSWORD`).

A command definition for Icinga 2 might look like this:

```
object CheckCommand "check_rocketchat_archive" {
  import "plugin-check-command"

  command = [ "/opt/rocketchat-archive/misc/check_archive.py" ]

  env.ARCHIVE_BASE_URL = "$base_url$"
  env.ARCHIVE_USERNAME = "$username$"
  env.ARCHIVE_PASSWORD = "$password$"
}
```

And the matching service definition:

```
apply Service "Rocketchat Archive" {
  import "generic-service"

  check_command = "check_rocketchat_archive"

  vars.base_url = "https://chat.rueckgr.at/archive/"
  vars.username = "..."
  vars.password = "..."

  assign where host.name == "alpha"
}
```

## Local development

In order to run the application on your local machine, take the following steps:

* Ensure Docker is installed.
* Use NVM to set up Node 20.x via `nvm use lts/iron`.
* Map the host name `mongo` to `127.0.0.1` using `/etc/hosts`.
* Fire up a local MongoDB instance using Docker: 

```docker run --name mongo -p 127.0.0.1:27017:27017 mongo:7.0.16 mongod --oplogSize 128 --replSet rs0 --storageEngine=wiredTiger```

* Set up the MongoDB replica set:

```docker exec -it mongo mongosh localhost/rocketchat --eval "rs.initiate({ _id: 'rs0', members: [ { _id: 0, host: 'localhost:27017' } ]})"```

* Use `mongodump` to create a dump
  of your production installation of Rocket.Chat (into a directory named `dump`).
* Place this directory on your local machine.
* From the directory containing that directory, invoke `mongorestore`.
* Start the backend (e.g., import the Gradle project into IntelliJ IDEA and invoke `MainKt`).
* Start the frontend (e.g., run `npm install` and `ROCKETCHAT_URL='...' npm run start`) from the `frontend` directory.
* Point your browser to http://localhost:4200.

## Remote debugging

The backend opens a debug port on port `5005` which is forwarded to port `42775` in `docker-compose.yml`.

This is intended to be used in combination with SSH's port forwarding feature
when connecting from your local machine to the Docker host:

```
ssh -L 42775:127.0.0.1:42775 <host>
```

You can then connect your debugger to `localhost:42775` to debug the running bot instance remotely.


