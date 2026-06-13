# ==========================================
# Stage 1: Build the application
# ==========================================
FROM eclipse-temurin:25-jdk AS builder

# Vaadin production build requires Node.js
RUN apt-get update && apt-get install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy maven wrapper and pom.xml to cache dependencies
COPY mvnw pom.xml ./
COPY .mvn .mvn/

# Ensure the maven wrapper script is executable (fixes Windows git clone permission issues)
RUN chmod +x mvnw

# Optional: go-offline to cache maven dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy the rest of the source code and frontend config files
COPY src src/
COPY package.json vite.config.ts tsconfig.json types.d.ts ./
COPY package-lock.json* ./

# Build the application with production profile
RUN ./mvnw clean package -Pproduction -DskipTests

# ==========================================
# Stage 2: Create the final minimal image
# ==========================================
FROM eclipse-temurin:25-jre

# Create a non-root user and group
RUN groupadd -g 1000 oraculum && \
    useradd -r -u 1000 -g oraculum -d /app oraculum

WORKDIR /app

# Copy the built jar from the builder stage and set ownership
COPY --from=builder --chown=oraculum:oraculum /app/target/*.jar app.jar

# Switch to the non-root user
USER 1000:1000

# Expose the default Spring Boot port
EXPOSE 8080

# Set default memory options. 
# MaxRAMPercentage sets the max heap based on container limit.
# Min/MaxHeapFreeRatio forces Java to aggressively return unused RAM to the OS!
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

