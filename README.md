# jdbc-kerberos

## Build
mvn clean compile assembly:single

# Prepare Keytab
ktutil:  addent -password -p elena@ENEROSORG.ONMICROSOFT.COM -k 2 -e aes128-cts-hmac-sha1-96
ktutil: wkt elena.keytab

# Init Token and Execure
sudo kinit -V elena@ENEROSORG.ONMICROSOFT.COM

sudo kinit -V -kt /home/sqlAdmin/elena.keytab  elena
export KRB5CCNAME=/tmp/krb5cc_1000
sudo java -jar target/sql-kerberos-jar-with-dependencies.jar

# docker

docker build -t sql-kerberos:1.0 .
docker tag sql-kerberos:1.0 acraccess.azurecr.io/sql-kerberos:1.0
docker push acraccess.azurecr.io/sql-kerberos:1.0