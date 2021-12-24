package com.msft.kerberos;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class SQLTest {

    public static void main(String[] args) {

        // Create datasource.
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName("SQLIAASEN.ENEROSORG.ONMICROSOFT.COM");
        ds.setPortNumber(1433);
        ds.setDatabaseName("testdb");
        ds.setIntegratedSecurity(true);
        ds.setAuthenticationScheme("JavaKerberos");
        
     
        do {
            try (Connection c = ds.getConnection(); Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("SELECT SUSER_SNAME()")) 
            {
                while (rs.next()) {
                    System.out.println("Authenticated User: " + rs.getString(1));
                }
            }
                // Handle any errors that may have occurred.
            catch (SQLException e) {
                e.printStackTrace();
            }
        } while (true);
    }
}