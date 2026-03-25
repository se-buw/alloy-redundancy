FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

# Remove the Yarn apt source whose GPG key (62D54FD4003F6525) has expired,
# which causes apt-get update to fail when installing devcontainer features.
RUN rm -f /etc/apt/sources.list.d/yarn.list
