FROM eclipse-temurin:21-jdk
EXPOSE 5000

COPY build/libs/*.jar .
CMD java -jar *.jar