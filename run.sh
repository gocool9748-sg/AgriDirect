#!/bin/sh
# Start AgriDirect (Linux / macOS). Needs Java 17+.
#   ./run.sh          start the app
#   ./run.sh check    run the self-test
#   ./run.sh web      start the website on http://localhost:8080
#   ./run.sh build    recompile AgriDirect.jar from source (needs a JDK)
set -e
cd "$(dirname "$0")"
case "$1" in
  build)
    rm -rf out && mkdir -p out
    javac -encoding UTF-8 -d out --release 17 $(find src/main/java -name "*.java")
    cp -r src/main/resources/* out/
    printf 'Main-Class: com.agrimarket.Main\nClass-Path: lib/mariadb-java-client-2.7.6.jar lib/h2-2.2.220.jar\n' > manifest.txt
    jar cfm AgriDirect.jar manifest.txt -C out . && rm manifest.txt
    echo "Built AgriDirect.jar" ;;
  check) java -cp "AgriDirect.jar:lib/*" com.agrimarket.tools.SystemCheck ;;
  web)   java -cp "AgriDirect.jar:lib/*" com.agrimarket.web.WebServer open $2 ;;
  *)     java -cp "AgriDirect.jar:lib/*" com.agrimarket.Main ;;
esac
