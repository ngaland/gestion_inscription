# Backend Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copier les fichiers Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Donner les permissions d'exécution à mvnw
RUN chmod +x ./mvnw

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src src

# Construire l'application
RUN ./mvnw clean package -DskipTests

# Exposer le port
EXPOSE 8086

# Lancer l'application
CMD ["java", "-jar", "target/gestin_inscription-0.0.1-SNAPSHOT.jar"]