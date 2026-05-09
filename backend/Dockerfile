FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/tiktoknas-backend-1.0.0.jar app.jar
RUN mkdir -p /data/covers /data/hls /videos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
