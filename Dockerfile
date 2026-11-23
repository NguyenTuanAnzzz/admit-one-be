FROM eclipse-temurin:17
COPY target/admit-one.jar /tmp/admit-one.jar
CMD ["java", "-jar", "/tmp/admit-one.jar"]