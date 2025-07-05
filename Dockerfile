# Usar OpenJDK 21 como imagen base
FROM openjdk:21-jdk-slim

# Establecer directorio de trabajo
WORKDIR /app

# Instalar Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copiar archivos de configuración de Maven
COPY pom.xml .

# Descargar dependencias (esto ayuda con el caching de Docker)
RUN mvn dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean compile

# Exponer el puerto que usa la aplicación
EXPOSE $PORT

# Comando para ejecutar la aplicación
# Render proporciona la variable de entorno PORT
CMD mvn exec:java -Dexec.mainClass=com.drawtogether.Main -Dexec.args="$PORT"
