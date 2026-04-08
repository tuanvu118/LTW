FROM maven:3.9.8-amazoncorretto-21

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src

EXPOSE 8080

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=pro"]
#CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]