FROM ubuntu:14.04

MAINTAINER Matthias Zimmermann <matthias.zimmermann@bsi-software.com>

# windows workaround to ensure access to internet from within container
RUN echo "nameserver 8.8.8.8" >> /etc/resolv.conf

# basics
RUN \
  apt-get update && apt-get upgrade -q -y && \
  apt-get install -y --no-install-recommends git make gcc libc6-dev curl ca-certificates software-properties-common

# install node
RUN \
  curl -sL https://deb.nodesource.com/setup_7.x | sudo -E bash - && \
  apt-get install -y nodejs python-pip python-dev build-essential git && \
  pip install --upgrade pip virtualenv

# install testrpc, solidity compiler and web3 js client
RUN \ 
  npm install --unsafe-perm -g ethereumjs-testrpc solc web3 && \
  apt-get clean

EXPOSE 8545

ENTRYPOINT ["testrpc"]
