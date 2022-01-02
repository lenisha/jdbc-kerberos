# Java JDBC apps connecting to SQL wih windows Auth


# User Setup 
- create user in Azure AD for Managed Domain tenant
- Grant access to user in `testdb`
```
CREATE LOGIN [ENEROSORG\dbuser] FROM WINDOWS
CREATE USER [ENEROSORG\dbuser] FOR LOGIN [ENEROSORG\dbuser];  
ALTER ROLE db_owner ADD MEMBER [ENEROSORG\dbuser];
```

## Domain Joined VM
```
kinit -V eneros@ENEROSORG.ONMICROSOFT.COM
----------------------------------------------------------------------------------

Using default cache: /tmp/krb5cc_1000
Using principal: dbuser@ENEROSORG.ONMICROSOFT.COM
Password for dbuser@ENEROSORG.ONMICROSOFT.COM: 
Authenticated to Kerberos v5

```
- Run SQL command

```
sqlcmd -E -S SQLIAASEN.ENEROSORG.ONMICROSOFT.COM -d testdb -Q "SELECT SUSER_SNAME();"
                                                                                                                     
----------------------------------------------------------------------------------
ENEROSORG\dbuser         
```

## Non Joined VM
## Install Kerberos utils and config
```
sudo apt-get update
sudo apt-get install krb5-user samba sssd sssd-tools libnss-sss libpam-sss ntp ntpdate realmd adcli
```

### Prepare Keytab

```sh
ktutil:  addent -password -p dbuser@ENEROSORG.ONMICROSOFT.COM -k 2 -e aes128-cts-hmac-sha1-96
ktutil: wkt dbuser.keytab
```

# Init Token and Execute SQL

```sh
sudo kinit -V -kt dbuser.keytab  dbuser

Using default cache: /tmp/krb5cc_1000
Using principal: dbuser@ENEROSORG.ONMICROSOFT.COM
Using keytab: dbuser.keytab
Authenticated to Kerberos v5

sqlcmd -E -S SQLIAASEN.ENEROSORG.ONMICROSOFT.COM -d testdb -Q "SELECT SUSER_SNAME();"
---------------------------------------------------------------------------------------
ENEROSORG\dbuser                                                                              
```

# Java
Our example uses JDBS driver java implemetation for integrated authentication with the Java Krb5LoginModule.
we specify `integratedSecurity=true` and `authenticationScheme=JavaKerberos `connection properties.

## Build
```
mvn clean compile assembly:single
```

## Java Test execute
export KRB5CCNAME=/tmp/krb5cc_1000
sudo java -jar target/sql-kerberos-jar-with-dependencies.jar

# Kubernetes

- build docker images
```
docker build -t sql-kerberos:1.0 .
docker tag sql-kerberos:1.0 acraccess.azurecr.io/sql-kerberos:1.0
docker push acraccess.azurecr.io/sql-kerberos:1.0
```
or
```
az acr build -r acraccess --image sql-kerberos:1.0  -f containers/dbapp/Dockerfile .

cd containers/sidecar
az acr build -r acraccess --image  kinit-sidecar:1.0   .

```
## Kubernetes setup

- create secret with keytab data
```
kubectl create secret generic keytab --from-file=./dbuser.keytab
```

- create configmap with kerberos config
```
kubectl create cm krb5config --from-file=./containers/krb5.conf
```

- create pod with two containers  - sidecar running kinit to refresh ticket and application 

```
kubectl apply -f containers/k8s-manifest.yaml
```

- verify logs 

```
 k logs kinit-dbapp -c kinit --tail=20

*** Waiting for 10 seconds
*** kinit at +2021-12-24 + kinit -V -k dbuser@ENEROSORG.ONMICROSOFT.COM 
Using default cache: /dev/shm/ccache
Using principal: dbuser@ENEROSORG.ONMICROSOFT.COM
Authenticated to Kerberos v5
Ticket cache: FILE:/dev/shm/ccache
Default principal: dbuser@ENEROSORG.ONMICROSOFT.COM

Valid starting     Expires            Service principal
12/24/21 05:36:25  12/24/21 15:36:25  krbtgt/ENEROSORG.ONMICROSOFT.COM@ENEROSORG.ONMICROSOFT.COM
        renew until 12/31/21 05:36:25
*** Waiting for 10 seconds
```

```
k logs kinit-dbapp -c dbapp --tail=10
Authenticated User: ENEROSORG\dbuser
Authenticated User: ENEROSORG\dbuser
Authenticated User: ENEROSORG\dbuser
Authenticated User: ENEROSORG\dbuser
```

### Keyvault setup
 az keyvault secret set --name dbuserkt --vault-name kvforkeytab  --file dbuser.keytab --encoding hex

k create configmap dbconfig --from-literal=SQL-SERVER=SQLIAASEN.ENEROSORG.ONMICROSOFT.COM --from-literal=DB-NAME=testdb


## References:
[Join an Ubuntu Linux virtual machine to an Azure Active Directory Domain Services managed domain](https://docs.microsoft.com/en-us/azure/active-directory-domain-services/join-ubuntu-linux-vm)

[Install sqlcmd ](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-ver15)

[Connecting a SQL Server client on Linux using Active Directory authentication](https://sqlsunday.com/2021/04/15/connecting-linux-using-ad-authentication/)

[Linux to Windows Authentication](https://www.sqlservercentral.com/blogs/linux-to-windows-authentication)
[Linux to Windows Authentication GitHub](https://github.com/fenngineering/GoSql.Kerberos)

[Kerberos Sidecar Container](https://cloud.redhat.com/blog/kerberos-sidecar-container)
[Kerberos Sidecar Container Github](https://github.com/edseymour/kinit-sidecar)

[Using Kerberos integrated authentication to connect to SQL Server](https://docs.microsoft.com/en-us/sql/connect/jdbc/using-kerberos-integrated-authentication-to-connect-to-sql-server?view=sql-server-ver15)

[Register a Service Principal Name for Kerberos Connections](https://docs.microsoft.com/en-us/sql/database-engine/configure-windows/register-a-service-principal-name-for-kerberos-connections?view=sql-server-ver15)

[AD(Active Directory) authentication for SQL Containers on Azure Kubernetes Service (AKS)](https://techcommunity.microsoft.com/t5/sql-server-blog/ad-active-directory-authentication-for-sql-containers-on-azure/ba-p/2745659)