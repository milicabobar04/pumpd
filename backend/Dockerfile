# Backend pumpd u kontejneru (pokreće ga docker-compose.yml iz korijena projekta).
# Dvije faze: prva pravi .jar (Maven), druga ga samo pokreće, bez Mavena i izvornog koda.

# ---------- 1. pravljenje .jar fajla ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# Prvo samo pom.xml i Maven wrapper: zavisnosti se preuzimaju u poseban sloj, pa se ne preuzimaju
# ponovo pri svakoj izmjeni koda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
# Testovi se ovdje ne pokreću: integracioni testovi traže Docker, a pokreću se prije spajanja (./mvnw test).
RUN ./mvnw -q -B -DskipTests package && cp target/*.jar /src/app.jar

# ---------- 2. pokretanje ----------
FROM eclipse-temurin:21-jre
# Običan korisnik umjesto root-a: ako bi neko preuzeo proces, nema administratorska prava u kontejneru.
RUN groupadd --system pumpd && useradd --system --gid pumpd --no-create-home pumpd \
    && mkdir -p /var/pumpd/slike && chown -R pumpd:pumpd /var/pumpd
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER pumpd
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
