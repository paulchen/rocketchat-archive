FROM debian:trixie-slim
RUN apt-get update && \
    apt-get -y dist-upgrade && \
    apt-get -y install --no-install-recommends curl adduser && \
    apt-get -y autoremove && \
    apt-get -y purge $(dpkg -l | grep '^rc' | awk '{print $2}') && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:21-jdk $JAVA_HOME $JAVA_HOME

RUN mkdir /app
ADD build/distributions/rocketchat-archive-backend-latest.tar /app

RUN addgroup --gid 1026 mygroup && adduser --disabled-password --ingroup mygroup --system myuser
USER myuser

WORKDIR /app/rocketchat-archive-backend-latest
CMD ["bin/rocketchat-archive-backend"]
