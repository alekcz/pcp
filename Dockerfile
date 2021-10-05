# Multi stage image for building pcp
# Stage 1: build pcp
FROM ghcr.io/graalvm/graalvm-ce:java8-21.0.0.2 as build

RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein --output /usr/local/bin/lein && \
    chmod a+x /usr/local/bin/lein && \
    gu install native-image

COPY . /app-src
WORKDIR /app-src

RUN bash build.sh && \
    ls -la target/

# Stage 2: final image
FROM adoptopenjdk:8-jre-hotspot-focal

COPY --from=build /app-src/target/pcp* /usr/local/bin/
COPY docker-entrypoint.sh /docker-entrypoint.sh

COPY resources/pcp-templates /usr/share/pcp-site

EXPOSE 9000
EXPOSE 3000

CMD ["/docker-entrypoint.sh"]
