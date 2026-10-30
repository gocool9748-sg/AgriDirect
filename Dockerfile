# AgriDirect website in a container (used by Render, Railway, Fly.io, or "docker build").
# Build: compile the Java sources with the JDBC drivers in lib/.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY src src
COPY lib lib
RUN mkdir out \
 && javac -encoding UTF-8 -d out --release 17 $(find src/main/java -name "*.java") \
 && cp -r src/main/resources/* out/

# Run: only the compiled classes, the drivers and the SQL scripts.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/out out
COPY lib lib
COPY sql sql
# The host sets PORT; HOST=0.0.0.0 makes the server reachable from outside the container.
ENV PORT=8080 HOST=0.0.0.0
# Show order times in Indian time (servers run on UTC).
ENV TZ=Asia/Kolkata
EXPOSE 8080
# First start creates the built-in database (data/) with the demo farmers, buyers and products.
CMD ["java", "-Duser.timezone=Asia/Kolkata", "-cp", "out:lib/*", "com.agrimarket.web.WebServer"]
