FROM eclipse-temurin:latest

COPY /target/pts-control-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","/app.jar"]
