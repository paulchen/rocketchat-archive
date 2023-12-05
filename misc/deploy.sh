#!/bin/bash

SCRIPT_DIR=`dirname "$0"`
SCRIPT_DIR=`realpath "$SCRIPT_DIR"`
BASE_DIR=`realpath "$SCRIPT_DIR/.."`

cd "$SCRIPT_DIR"
if [ ! -f deploy.conf ]; then
	echo deploy.conf does not exist
	exit 1
fi

. deploy.conf

. ~/.nvm/nvm.sh || exit 1
nvm install lts/hydrogen || exit 1

docker pull debian:bookworm-slim || exit 1
docker pull eclipse-temurin:17-jdk || exit 1
docker pull nginx:latest || exit 1

cd "$BASE_DIR"

git pull || exit 1

cd "$BASE_DIR/frontend"

npm install || exit 1
ROCKETCHAT_URL="$ROCKETCHAT_URL" npm run build -- --base-href="$BASE_HREF" || exit 1

cd "$BASE_DIR/backend"

./gradlew distTar || exit 1

cd "$BASE_DIR"

docker compose build --no-cache || exit 1

if [ "$1" != "--no-systemd" ]; then
	sudo systemctl restart "$SYSTEMD_UNIT" || exit 1
fi

