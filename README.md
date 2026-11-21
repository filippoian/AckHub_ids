# HackHub

Applicazione Spring Boot per la gestione di hackathon: iscrizione team, submission, valutazioni, supporto/mentoring e segnalazioni disciplinari. Espone sia una REST API sia una CLI interattiva sullo stesso backend.

## Requisiti

- JDK 17 o superiore
- Maven (incluso come wrapper, non serve installarlo a parte)

## Avvio

```sh
sh mvnw spring-boot:run
```

L'API REST è disponibile su `http://localhost:8080`.

Per avviare anche la CLI interattiva nello stesso processo (richiede un terminale interattivo):

```sh
java -jar target/Hackhub-0.0.1-SNAPSHOT.jar --hackhub.cli.enabled=true
```

Da IntelliJ: eseguire direttamente `it.unicam.hackhub.HackhubApplication`. Su Windows usare `mvnw.cmd` al posto di `sh mvnw`.

## Build e test

```sh
sh mvnw clean test      # esegue la suite di test (JUnit, database H2 in memoria)
sh mvnw package          # genera il JAR eseguibile in target/Hackhub-0.0.1-SNAPSHOT.jar
```

## Test Postman

Importa la [collection HackHub](postman/HackHub.postman_collection.json) e l’[environment HackHub - Local](postman/HackHub.postman_environment.json). Esegui l’intera collection in ordine. Ogni run crea account, team ed eventi propri. Le verifiche di logout e disattivazione usano gli account di prova creati durante l’esecuzione.

Avvia un’istanza locale separata con un database H2 in memoria.

```sh
sh mvnw spring-boot:run \
  '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=18080 --spring.datasource.url=jdbc:h2:mem:hackhub_postman;DB_CLOSE_DELAY=-1 --spring.jpa.hibernate.ddl-auto=create-drop'
```

Nel Collection Runner imposta `baseUrl` a `http://127.0.0.1:18080`. Puoi eseguire la stessa collection da terminale con Newman. [Documentazione Newman](https://learning.postman.com/docs/collections/using-newman-cli/installing-running-newman)

```sh
npx --yes newman@6.2.1 run postman/HackHub.postman_collection.json \
  -e postman/HackHub.postman_environment.json \
  --env-var baseUrl=http://127.0.0.1:18080 --bail --timeout-script 65000
```

Esegui Newman o Postman nello stesso fuso orario del backend. La suite impiega circa un minuto perché attende l’inizio e la fine della gara prima di richiedere i cambi di fase.

## Persistenza

Il database di runtime è un file H2 in `data/hackhub` (creato automaticamente al primo avvio, schema aggiornato da Hibernate a ogni riavvio). I test usano invece un database H2 in memoria, isolato dai dati di runtime.

## Struttura del progetto

```
src/main/java/it/unicam/hackhub/
├── model/          entità di dominio, enum, pattern State per lo stato dell'hackathon
├── repository/      interfacce di persistenza, con implementazioni in-memory e JPA/Spring Data
├── security/        hashing password, generazione codici di recupero, invalidazione sessioni
├── external/        interfacce e stub per sistemi esterni (pagamento premio, calendario)
├── controller/       logica applicativa (le regole di dominio, indipendenti da REST/CLI)
├── web/              REST API: controller HTTP, DTO, sessioni via header, gestione errori
├── cli/              interfaccia a riga di comando sullo stesso backend
└── bootstrap/        dati di esempio caricati all'avvio (DataSeeder)
```
