FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew ./
COPY gradle/wrapper/ gradle/wrapper/
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/
RUN chmod +x gradlew && ./gradlew -p backend dependencies --no-daemon
COPY backend/src/ backend/src/
RUN ./gradlew -p backend installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /opt/app
COPY --from=build /workspace/backend/build/install/let-it-out-backend/ ./
ENV APP_ENV=production
USER 65532:65532
EXPOSE 8080
ENTRYPOINT ["/opt/app/bin/let-it-out-backend"]
