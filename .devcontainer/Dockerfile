FROM mcr.microsoft.com/devcontainers/java:21

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends ripgrep \
    && rm -rf /var/lib/apt/lists/*

USER vscode
