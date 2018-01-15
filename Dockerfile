FROM openjdk:8-alpine
MAINTAINER mna30547
COPY ./target/UDPInterface-1.0.0-SNAPSHOT-jar-with-dependencies /home/UDPInterface-1.0.0-SNAPSHOT-jar-with-dependencies
COPY ./src/main/resources/libper-xer-codec.so /home/libper-xer-codec.so
RUN mkdir /home/config
COPY ./src/main/resources/config/settings.properties /home/config/settings.properties
WORKDIR /home
CMD ["java","-Djava.library.path=/home/", "-jar", "/home/UDPInterface-1.0.0-SNAPSHOT-jar-with-dependencies"] 
