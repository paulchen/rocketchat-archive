FROM debian:bookworm-slim
RUN apt-get update
RUN apt-get -y dist-upgrade
RUN apt-get -y autoremove
RUN apt-get -y purge $(dpkg -l | grep '^rc' | awk '{print $2}')
RUN apt-get clean

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:17-jdk $JAVA_HOME $JAVA_HOME

RUN mkdir /app
ADD build/distributions/rocketchat-archive-backend-latest.tar /app

RUN addgroup --gid 1026 mygroup && adduser --disabled-password --ingroup mygroup --system myuser
USER myuser

WORKDIR /app/rocketchat-archive-backend-latest
CMD ["bin/rocketchat-archive-backend"]
