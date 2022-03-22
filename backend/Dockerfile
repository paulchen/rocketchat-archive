FROM openjdk:17-jdk-slim
RUN mkdir /app
ADD build/distributions/rocketchat-archive-backend-latest.tar /app

RUN addgroup --gid 1026 mygroup && adduser --disabled-password --ingroup mygroup --system myuser
USER myuser

WORKDIR /app/rocketchat-archive-backend-latest
CMD ["bin/rocketchat-archive-backend"]
