FROM eclipse-temurin:21-jdk AS jdk
RUN apt-get update && \
    apt-get -y install --no-install-recommends git

COPY . /opt/app
WORKDIR /opt/app/backend
RUN ./gradlew installDist

FROM debian:trixie-slim
RUN apt-get update && \
    apt-get -y dist-upgrade && \
    apt-get -y install --no-install-recommends curl adduser && \
    apt-get -y autoremove && \
    apt-get -y purge $(dpkg -l | grep '^rc' | awk '{print $2}') && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=jdk $JAVA_HOME $JAVA_HOME
COPY --from=jdk /opt/app/backend/build/install/rocketchat-archive-backend/ /app/rocketchat-archive-backend/

RUN addgroup --gid 1026 mygroup && adduser --disabled-password --ingroup mygroup --system myuser
USER myuser

WORKDIR /app/rocketchat-archive-backend
CMD ["bin/rocketchat-archive-backend"]
