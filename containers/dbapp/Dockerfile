FROM maven:3.8.4-jdk-11-slim as maven

COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn clean compile assembly:single

FROM openjdk:11-jre-bullseye 
WORKDIR /app
COPY --from=maven target/sql-kerberos-jar-with-dependencies.jar /app/sql-kerberos.jar

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update
RUN apt-get install -y krb5-config
RUN apt-get install -y krb5-user
RUN mkdir /krb5 && chmod 755 /krb5

VOLUME ["/krb5","/dev/shm","/etc/krb5.conf.d"]

ENV KRB5CCNAME=/dev/shm/ccache
CMD ["java", "-jar", "sql-kerberos.jar"]