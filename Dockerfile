FROM  docker.io/nvidia/cuda:11.3.1-cudnn8-runtime-ubuntu18.04
ENV \
  LANG=C.UTF-8 \
  JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 \
  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/cuda/lib64

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update &&\
  apt-get install -y --no-install-recommends \
  wget \
  zip \
  unzip \
  ca-certificates \
  build-essential \
  curl \
  libcurl4-openssl-dev \
  libssl-dev \
  libgomp1 \
  openjdk-11-jdk \
  ca-certificates-java

RUN  wget -q -O /tmp/sbt-1.5.5.tgz https://github.com/sbt/sbt/releases/download/v1.5.5/sbt-1.5.5.tgz &&\
  tar -zxf /tmp/sbt-1.5.5.tgz --strip=1 -C /usr

WORKDIR /jar

ADD build.sbt ./
ADD src ./src/
ADD model ./model/
ADD project/build.properties ./project/
ADD project/plugins.sbt ./project/

RUN sbt clean assembly

ENTRYPOINT exec java $JVM_OPTS \
-Dport=8080 \
-Dbatch=16 \
-Dtimeout=10000 \
-Ddim=100 \
-Dsample=10000 \
-DnpyFile=./model/10000-100.npy \
-DtakeSpinCountDelay=5 \
-Xmx8G \
-Xms8G \
-DLOG_LEVEL=${LOG_LEVEL} \
-jar ./target/scala-2.12/flasma.jar

