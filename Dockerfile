FROM openjdk:17
EXPOSE 5000

./gradlew build
COPY build/libs/*.jar .
CMD java -jar *.jar
