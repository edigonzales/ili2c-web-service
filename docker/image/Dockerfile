FROM tomcat:10.1.40-jre21-temurin-noble

RUN useradd -m -u 1001 tomcatuser

RUN chown -R tomcatuser:0 /usr/local/tomcat && \
    chmod -R 0775 /usr/local/tomcat && \
    ls -la /usr/local/tomcat

RUN rm -rf /usr/local/tomcat/webapps/*

COPY tmp/ili2c-web-service-*.war /usr/local/tomcat/webapps/ROOT.war

ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=80.0"

EXPOSE 8080

USER tomcatuser

CMD ["catalina.sh", "run"]