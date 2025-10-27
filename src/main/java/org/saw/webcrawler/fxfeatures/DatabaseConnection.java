package org.saw.webcrawler.fxfeatures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    // root database
    private static final String ROOT_DATABASE_URL = "jdbc:mysql://localhost:3306/";
    private static final String ROOT_USER = "root";
    private static final String ROOT_PASSWORD = "";

    // Instance fields
    private String saveUUID = "";
    private String databaseUrl = "";
    private String databaseUser = "";
    private String databasePassword = "";
    private String generatedatabaseName = "";
    private String generatedatabaseUser = "";
    private String generatedatabasePassword = "";
    private String getUuid = "";
    private String storeDatabaseName = "";

    // Windows username
    private final String winUsername = System.getProperty("user.name");

    // Process for UUID
    private Process uuidProcess;
    private final ProcessBuilder CMDFORUUID =
            new ProcessBuilder("cmd.exe", "/c", "wmic path win32_computersystemproduct get UUID");
    private final ProcessBuilder POWFORUUID =
            new ProcessBuilder("powershell.exe", "-Command",
                    "Get-CimInstance Win32_ComputerSystemProduct | Select-Object UUID");

    // Getters for other classes
    public String getDatabaseUrl() { return databaseUrl; }
    public String getDatabaseUser() { return databaseUser; }
    public String getDatabasePassword() { return databasePassword; }

    /**
     * Checking uuid available or not, database, table
     */
    public boolean checkingUser() {
        try (Connection con = DriverManager.getConnection(ROOT_DATABASE_URL, ROOT_USER, ROOT_PASSWORD)) {
            Class.forName("com.mysql.cj.jdbc.Driver");

            generatedatabaseName = winUsername + "_" + getRandomNumber(10000, 99999);
            generatedatabaseUser = winUsername + getRandomNumber(10000, 99999);
            generatedatabasePassword = winUsername + getRandomNumber(100000, 999999);
            getUuid = getSaveUUID();

            // Create/check main DB
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS webcrawler");
                stmt.executeUpdate("USE webcrawler");
                readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Database webcrawler created/checked Successfully");
                System.out.println(readInfo.timestamp() + "DatabaseConnection file: Database webcrawler created/checked Successfully");
            }

            // Create/check table
            try (Statement stmt = con.createStatement()) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS available (" +
                        "id INT NOT NULL AUTO_INCREMENT," +
                        "uuid VARCHAR(255)," +
                        "username VARCHAR(255)," +
                        "password VARCHAR(255)," +
                        "db VARCHAR(255)," +
                        "PRIMARY KEY(id)" +
                        ");";
                stmt.executeUpdate(createTableSQL);
                readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Table created/checked Successfully");
                System.out.println(readInfo.timestamp() + "DatabaseConnection file: Table created/checked Successfully");
            }

            // Query by UUID
            try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM available WHERE uuid = ?")) {
                pstmt.setString(1, getUuid);
                ResultSet rs = pstmt.executeQuery();

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Successfully Found....Db, Username, Password");
                    System.out.println(readInfo.timestamp() + "DatabaseConnection file: Successfully Found....Db, Username, Password");
                    String uuid = rs.getString("uuid");
                    databaseUser = rs.getString("username");
                    databasePassword = rs.getString("password");
                    databaseUrl = "jdbc:mysql://localhost:3306/" + rs.getString("db");
                    System.out.println("Found Existing user");
                    System.out.println("UUID: " + uuid);
                    System.out.println("User: " + databaseUser);
                    System.out.println("Password: " + databasePassword);
                    System.out.println("DB URL: " + databaseUrl);
                    System.out.println("End of Existing user");
                }

                if (!found) {
                    readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: UUID not found");
                    System.out.println(readInfo.timestamp() + "DatabaseConnection file: UUID not found");
                    System.out.println("Generated DB Name: " + generatedatabaseName);
                    System.out.println("Generated DB User: " + generatedatabaseUser);
                    System.out.println("Generated DB Password: " + generatedatabasePassword);
                    createDatabaseAndTable(con);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create new user DB entry if UUID not found
     */
    private void createDatabaseAndTable(Connection con) {
        try {
            // Insert new uuid, username, password
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO available (uuid, username, password, db) VALUES (?,?,?,?)")) {
                stmt.setString(1, getUuid);
                stmt.setString(2, generatedatabaseUser);
                stmt.setString(3, generatedatabasePassword);
                stmt.setString(4, generatedatabaseName);
                stmt.executeUpdate();
            }
            storeDatabaseName = generatedatabaseName;
            databaseUrl = "jdbc:mysql://localhost:3306/" + generatedatabaseName;

            try (Statement stmtDb = con.createStatement()) {
                stmtDb.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + storeDatabaseName + "`");
                stmtDb.executeUpdate("USE `" + storeDatabaseName + "`");
                System.out.println("Created/checked user database: " + storeDatabaseName);
            }

            try (Statement stmtFlush = con.createStatement()) {
                String createUserSQL = "CREATE USER IF NOT EXISTS '" + generatedatabaseUser +
                        "'@'localhost' IDENTIFIED BY '" + generatedatabasePassword + "'";
                stmtFlush.executeUpdate(createUserSQL);

                String grantSQL = "GRANT ALL PRIVILEGES ON `" + storeDatabaseName +
                        "`.* TO '" + generatedatabaseUser + "'@'localhost'";
                stmtFlush.executeUpdate(grantSQL);

                stmtFlush.executeUpdate("FLUSH PRIVILEGES");
                System.out.println("User " + generatedatabaseUser + " created with access to DB " + storeDatabaseName);
            }

            try (Statement stmtTable = con.createStatement()) {
                String createUserTableSQL =
                        "CREATE TABLE IF NOT EXISTS data (" +
                                "id INT NOT NULL AUTO_INCREMENT," +
                                "keyword VARCHAR(255)," +
                                "times INT DEFAULT 0," +
                                "link TEXT," +
                                "link_domain TEXT," +
                                "link_default TEXT," +
                                "image LONGBLOB," +
                                "found BOOLEAN DEFAULT FALSE,"+
                                "crawl_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "PRIMARY KEY(id)" +
                                ")";
                stmtTable.executeUpdate(createUserTableSQL);
                System.out.println("Table 'data' created/checked Successfully in DB " + storeDatabaseName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get UUID (WMIC → fallback PowerShell)
     */
    public String getSaveUUID() {
        try {
            uuidProcess = CMDFORUUID.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(uuidProcess.getInputStream()));
            reader.readLine();
            reader.readLine();
            String uuid = reader.readLine();
            if (uuid != null) uuid = uuid.trim();
            int exitCode = uuidProcess.waitFor();
            if (uuid != null && !uuid.isEmpty() && exitCode == 0) {
                saveUUID = uuid;
                readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Saved UUID (WMIC): " + saveUUID);
                System.out.println(readInfo.timestamp() + "DatabaseConnection file: Saved UUID (WMIC): " + saveUUID);
                return saveUUID;
            }

            uuidProcess = POWFORUUID.start();
            BufferedReader pwReader = new BufferedReader(new InputStreamReader(uuidProcess.getInputStream()));
            pwReader.readLine();
            pwReader.readLine();
            pwReader.readLine();
            String pwUuid = pwReader.readLine();
            if (pwUuid != null) pwUuid = pwUuid.trim();
            int psExitCode = uuidProcess.waitFor();
            if (pwUuid != null && !pwUuid.isEmpty() && psExitCode == 0) {
                saveUUID = pwUuid;
                readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Saved UUID (PowerShell): " + saveUUID);
                System.out.println(readInfo.timestamp() + "DatabaseConnection file: Saved UUID (PowerShell): " + saveUUID);
            } else {
                readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Failed to retrieve UUID with both WMIC and PowerShell.");
                System.err.println("DatabaseConnection file: Failed to retrieve UUID with both WMIC and PowerShell.");
            }
        } catch (IOException | InterruptedException ex) {
            readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection file: Error at getSaveUUID: " + ex.getMessage());
            ex.printStackTrace();
        }
        return saveUUID;
    }

    /**
     * Random number generator
     */
    private int getRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

//            Establish connection to database
//    public Connection getConnectionForReadWrite(String dbUrl, String username, String password) throws SQLException {
//        return DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
//    }
}
