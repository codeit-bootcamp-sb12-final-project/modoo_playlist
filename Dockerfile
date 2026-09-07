FROM amazoncorretto:17 AS builder
WORKDIR /app

COPY gradle ./gradle
COPY gradlew ./gradlew
COPY build.gradle settings.gradle ./
COPY core/build.gradle ./core/
COPY infra/build.gradle ./infra/
COPY module-api/build.gradle ./module-api/
COPY module-batch/build.gradle ./module-batch/
COPY module-realtime/build.gradle ./module-realtime/
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

COPY core/src ./core/src
COPY infra/src ./infra/src
COPY module-api/src ./module-api/src
COPY module-batch/src ./module-batch/src
COPY module-realtime/src ./module-realtime/src

ARG MODULE
RUN ./gradlew :${MODULE}:bootJar -x test --no-daemon \
 && cp ${MODULE}/build/libs/*.jar /app/app.jar

FROM amazoncorretto:17-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder --chown=app:app /app/app.jar ./app.jar
USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ARG PORT=8080
EXPOSE ${PORT}
ENTRYPOINT ["sh","-c","java $JVM_OPTS -jar /app/app.jar"]