FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
    && find target -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' \
        -exec cp '{}' /workspace/app.jar ';' \
    && test -f /workspace/app.jar

FROM eclipse-temurin:21-jre AS runtime

ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Taipei"

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app:app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
