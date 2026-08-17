FROM eclipse-temurin:25-jre

WORKDIR /opt/mabillon

COPY build/libs/mabillon-*.jar app.jar
COPY docs/archive docs/archive

RUN mkdir -p /opt/mabillon/data /opt/mabillon/sips \
    && useradd --system --uid 10001 --create-home mabillon \
    && chown -R mabillon:mabillon /opt/mabillon

USER mabillon

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75" \
    MABILLON_ENVIRONMENT=production \
    MABILLON_STORAGE_ROOT=/opt/mabillon/data \
    MABILLON_SIP_ROOT=/opt/mabillon/sips \
    SERVER_FORWARD_HEADERS_STRATEGY=framework

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/mabillon/app.jar"]
