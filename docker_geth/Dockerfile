FROM ubuntu:14.04

MAINTAINER Matthias Zimmermann <matthias.zimmermann@bsi-software.com>

# windows workaround to ensure access to internet from within container
RUN echo "nameserver 8.8.8.8" >> /etc/resolv.conf

# basics
RUN \
  apt-get update && apt-get upgrade -q -y && \
  apt-get install -y --no-install-recommends git make gcc libc6-dev curl ca-certificates software-properties-common

# get solc (solidity compiler binary)
RUN \
  add-apt-repository ppa:ethereum/ethereum && \
  apt-get update && \
  apt-get install -y --no-install-recommends solc

# install go
ENV GOLANG_VERSION 1.7.5
ENV GOLANG_DOWNLOAD_URL https://golang.org/dl/go$GOLANG_VERSION.linux-amd64.tar.gz
ENV GOLANG_DOWNLOAD_SHA256 2e4dd6c44f0693bef4e7b46cc701513d74c3cc44f2419bf519d7868b12931ac3

RUN \ 
  curl -fsSL "$GOLANG_DOWNLOAD_URL" -o golang.tar.gz && \
  echo "$GOLANG_DOWNLOAD_SHA256  golang.tar.gz" | sha256sum -c - && \
  tar -C /usr/local -xzf golang.tar.gz && \
  rm golang.tar.gz

ENV GOPATH=/go 
ENV GOROOT=/usr/local/go 
ENV PATH=${PATH}:${GOROOT}/bin:${GOPATH}/bin

# clone geth
RUN \
  git clone -b release/1.5 --depth 1 https://github.com/ethereum/go-ethereum

# patch geth
# http://ethereum.stackexchange.com/questions/11407/minimize-mining-times-for-private-blockchain
COPY block_validator.go /go-ethereum/core
COPY worker.go /go-ethereum/miner

# install patched geth
RUN \
  (cd go-ethereum && make geth) && \
  cp go-ethereum/build/bin/geth /geth && \
  apt-get remove -y git make gcc libc6-dev curl && \
  apt-get clean && \
  rm -rf /go-ethereum

COPY LocalGenesis.json /
COPY LocalPassword.txt /

# setup local private network with two initial accounts
RUN \
  mkdir /root/.ethash && \
  /geth init LocalGenesis.json > GethInit.txt && \
  /geth --password LocalPassword.txt account new >> GethInit.txt && \
  /geth --password LocalPassword.txt account new >> GethInit.txt && \
  /geth makedag 0 /root/.ethash

EXPOSE 8545
EXPOSE 30303

ENTRYPOINT ["/geth"]

# unlock coinbase, start mining, let geth listen to rpc requests and ensure it remains a local standalone client 
CMD ["--password","LocalPassword.txt","--unlock","0","--mine","--rpc","--rpcaddr","0.0.0.0","--maxpeers","0","--nodiscover"]
