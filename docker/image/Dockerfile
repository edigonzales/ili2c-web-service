FROM tomcat:10.1.40-jre21-temurin-noble

RUN rm -rf /usr/local/tomcat/webapps/*

COPY tmp/ili2c-web-service-*.war /usr/local/tomcat/webapps/ROOT.war

ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=80.0"

EXPOSE 8080

CMD ["catalina.sh", "run"]