# jdbc-kerberos

## Build
mvn clean compile assembly:single

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


## Java execute
export KRB5CCNAME=/tmp/krb5cc_1000
sudo java -jar target/sql-kerberos-jar-with-dependencies.jar

# docker

docker build -t sql-kerberos:1.0 .
docker tag sql-kerberos:1.0 acraccess.azurecr.io/sql-kerberos:1.0
docker push acraccess.azurecr.io/sql-kerberos:1.0


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