FROM openjdk:8-alpine
MAINTAINER mna30547
COPY ./target/DialogHandler-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/DialogHandler-0.0.1-SNAPSHOT-jar-with-dependencies.jar
COPY ./src/main/resources/libper-xer-codec.so /home/libper-xer-codec.so
RUN mkdir /home/config
COPY ./src/main/resources/config/settings.properties /home/config/settings.properties
WORKDIR /home
CMD ["java","-Djava.library.path=/home/", "-jar", "/home/DialogHandler-0.0.1-SNAPSHOT-jar-with-dependencies.jar"] 
