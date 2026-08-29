FROM eclipse-temurin:17-jdk

WORKDIR /sandbox

RUN useradd -m runner

USER runner

CMD ["java", "-version"]