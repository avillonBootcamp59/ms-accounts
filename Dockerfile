FROM openjdk:11-jdk-slim
VOLUME /tmp
COPY target/ms-accounts-0.0.1-SNAPSHOT.jar java-app.jar
ENTRYPOINT ["java","-jar","/app.jar"]