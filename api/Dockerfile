FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY protos protos
COPY api api
WORKDIR /src/api
RUN ./gradlew --no-daemon -q installDist -x test

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/api/build/install/api /app
EXPOSE 8080
CMD ["/app/bin/api"]
