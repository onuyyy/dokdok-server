# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1. Copy Gradle wrapper files (rarely change)
COPY gradle ./gradle
COPY gradlew ./
COPY gradle.properties* ./

# 2. Copy dependency definition files (change less frequently)
COPY build.gradle settings.gradle ./

# 3. Download dependencies (cached if dependencies don't change)
RUN ./gradlew dependencies --no-daemon

# 4. Copy source code (changes most frequently)
COPY src ./src

# 5. Build application (only reruns if source code changes)
RUN ./gradlew build -x test --no-daemon --parallel

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# Copy built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]