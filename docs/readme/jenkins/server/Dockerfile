FROM eclipse-temurin:27-jdk AS test-jdk
FROM node:24-bookworm-slim AS node
FROM docker:29-cli AS docker-cli
FROM jenkins/jenkins:lts-jdk21

USER root

COPY --from=test-jdk /opt/java/openjdk /opt/jdk-27
COPY --from=node /usr/local/ /usr/local/
COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker-cli /usr/local/libexec/docker/cli-plugins/docker-compose /usr/local/libexec/docker/cli-plugins/docker-compose

RUN npm install -g allure

USER jenkins
