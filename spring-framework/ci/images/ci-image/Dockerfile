FROM ubuntu:jammy-20240111

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
RUN ./setup.sh

ENV JAVA_HOME /opt/openjdk/java17
ENV JDK17 /opt/openjdk/java17
ENV JDK21 /opt/openjdk/java21
ENV JDK22 /opt/openjdk/java22

ENV PATH $JAVA_HOME/bin:$PATH
