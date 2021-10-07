FROM clojure:openjdk-16-lein-2.9.7-slim-buster as builder

WORKDIR /usr/pcp

COPY resources/pcp-templates /var/pcp/demo
COPY src/ /usr/pcp/src
COPY project.clj /usr/pcp/project.clj
RUN lein deps
RUN lein build-server


# use clean base image
FROM openjdk:16-slim-buster

EXPOSE 9000

# In scrict mode (default) the path must start with SERVER_ROOT which is /var/pcp by default
# when strict mode is off the path is set by http header in the request
# ENV STRICT 0 

WORKDIR /var/pcp
COPY --from=builder /usr/pcp/target/pcp-server.jar /var/pcp/pcp-server.jar
CMD ["java","-jar","/var/pcp/pcp-server.jar"]

# building the image
# docker build -t pcp:v1 .

# running the image
# docker run -p 9000:9000 --name=pcp-run -v ~/git/unclejoe:/var/pcp/default pcp:v1