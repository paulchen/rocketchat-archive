FROM openjdk:11-slim as build
WORKDIR /usr/local/app
COPY ./ /usr/local/app/
RUN ./gradlew dependencies
RUN ./gradlew distTar

FROM openjdk:11-jre-slim
RUN mkdir /app
ADD build/distributions/rocketchat-archive-backend.tar /app

WORKDIR /app/rocketchat-archive-backend
CMD ["bin/rocketchat-archive-backend"]
