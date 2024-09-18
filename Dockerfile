FROM debian:bookworm AS build
RUN apt-get update && apt-get install -y default-jdk-headless maven git
WORKDIR /opt/pub2agents
COPY . .
RUN git clone https://github.com/edamontology/pubfetcher.git && cd pubfetcher/ && git checkout develop && mvn clean install
RUN git clone https://github.com/edamontology/edammap.git && cd edammap/ && git checkout develop && mvn clean install
RUN mvn clean install

FROM debian:bookworm
RUN apt-get update && apt-get install -y default-jre firefox-esr
COPY --from=build /opt/pub2agents/target /opt/pub2agents
COPY --from=build /opt/pub2agents/edammap/doc/EDAM_1.25.owl /opt/pub2agents/edammap/doc/bioagents.idf /opt/pub2agents/edammap/doc/bioagents.stemmed.idf /opt/pub2agents/
WORKDIR /var/lib/pub2agents
EXPOSE 8080/tcp
CMD ["java", "-jar", "/opt/pub2agents/pub2agents-server-1.1.2-SNAPSHOT.jar", "-b", "http://0.0.0.0:8080", "--httpsProxy", "-e", "/opt/pub2agents/EDAM_1.25.owl", "-f", "files", "--db", "server.db", "--idf", "/opt/pub2agents/bioagents.idf", "--idfStemmed", "/opt/pub2agents/bioagents.stemmed.idf", "--bioagents", "bioagents.json", "--log", "/var/log/pub2agents"]
