package org.saw.webcrawler.fxfeatures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Random;

/**
 * For managing database connections and user setup
 */
public class DatabaseConnection {
    /**
     * JDBC URL for connecting to the MySQL server
     */
    private static final String ROOT_DATABASE_URL = ""; //your db url here

    /**
     * Administrative MySQL username
     */
    private static final String ROOT_USER = ""; //your db user here

    /**
     * Administrative MySQL password
     */
    private static final String ROOT_PASSWORD = ""; //your db password here


    /**
     * JDBC URL for this machine user database
     */
    private String databaseUrl = "";

    /**
     * Username for this machine user database
     */
    private String databaseUser = "";

    /**
     * password for this machine user database
     */
    private String databasePassword = "";


    /**
     *  Generated MySQL database name
     */
    private String generatedatabaseName = "";

    /**
     * Generated MySQL username
     */
    private String generatedatabaseUser = "";

    /**
     * Generated MySQL user password
     */
    private String generatedatabasePassword = "";

    /**
     * Store the database name
     */
    private String storeDatabaseName = "";

    /**
     * Store machine UUID
     */
    private String saveUUID = "";


    // run-once guard for admin setup only (NOT for credentials load)
    private static boolean checked = false;

    /**
     * Get the Windows username use in generated database/usernames/passwords
     */
    private final String winUsername = System.getProperty("user.name");

    /**
     * Process instance used for executing the PowerShell UUID command
     */
    private Process uuidProcess;

    /**
     * PowerShell command to get the machine UUID
     */
    private final ProcessBuilder POWFORUUID =
            new ProcessBuilder("powershell.exe", "-Command",
                    "Get-CimInstance Win32_ComputerSystemProduct | Select-Object UUID");

    /**
     * Getters
     * @return database url
     */
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * Getters
     * @return database user
     */
    public String getDatabaseUser() {
        return databaseUser;
    }

    /**
     * Getters
     * @return database password
     */
    public String getDatabasePassword() {
        return databasePassword;
    }

    /**
     * Getters
     * @return database name
     */
    public String getStoreDatabaseName() {
        return storeDatabaseName;
    }


    /**
     * If UUID not found, create new user database, table, passwords for this machine
     * If UUID found, load existing credentials
     * @return true if initialization successful, false on error
     */
    public boolean checkingUser() {
        // Ensure single-threaded execution of database setup
        synchronized (DatabaseConnection.class) {
            try {
                // Ensure driver is available
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException ignore) {
                }

                // Connected using admin credentials
                try (Connection con = DriverManager.getConnection(ROOT_DATABASE_URL, ROOT_USER, ROOT_PASSWORD)) {


                    //Ensure one-time admin DB and table setup
                    if (!checked) {
                        // Create/check main admin DB
                        try (Statement stmt = con.createStatement()) {
                            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS webcrawler");
                            stmt.executeUpdate("USE webcrawler");
                            readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: DB webcrawler ready");
                            System.out.println(readInfo.timestamp() + "DatabaseConnection: DB webcrawler ready");
                        }

                        // Create/check available table
                        try (Statement stmt = con.createStatement()) {
                            String createTableSQL = """
                        CREATE TABLE IF NOT EXISTS available (
                          id INT NOT NULL AUTO_INCREMENT,
                          uuid VARCHAR(255),
                          username VARCHAR(255),
                          email VARCHAR(255) UNIQUE,
                          password VARCHAR(255),
                          password_hash VARCHAR(255),
                          db VARCHAR(255),
                          otp_hash VARCHAR(255),
                          otp_expires_at DATETIME,
                          otp_request_count INT UNSIGNED NOT NULL DEFAULT 0,
                          otp_last_request_at DATETIME,
                          last_login_at DATETIME,
                          PRIMARY KEY(id),
                          KEY idx_db (db),
                          KEY idx_otp_expires_at (otp_expires_at)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """;
                            stmt.executeUpdate(createTableSQL);
                            readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: table 'available' ready");
                            System.out.println(readInfo.timestamp() + "DatabaseConnection: table 'available' ready");
                        }

                        checked = true;
                    } else {
                        // Ensure correct database selected
                        try (Statement stmt = con.createStatement()) {
                            stmt.executeUpdate("USE webcrawler");
                        }
                    }

                    // Try to load existing credentials by UUID
                    boolean found = false;
                    try (PreparedStatement pstmt = con.prepareStatement(
                            "SELECT * FROM available WHERE uuid = ? LIMIT 1")) {
                        pstmt.setString(1, saveUUID);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                // Existing user found
                                found = true;
                                storeDatabaseName = rs.getString("db");
                                databaseUser = rs.getString("username");
                                databasePassword = rs.getString("password");
                                databaseUrl = "" + rs.getString("db"); //your db url here + storeDatabaseName;
                                readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: existing user found");
                            }
                        }
                    }

                    // If not found, create new user database, user, password for this machine
                    if (!found) {
                        generatedatabaseName = winUsername + "_" + getRandomNumber(10000, 99999);
                        generatedatabaseUser = winUsername + getRandomNumber(10000, 99999);
                        generatedatabasePassword = winUsername + getRandomNumber(100000, 999999);
                        readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: UUID not found, creating user DB");
                        System.out.printf("Generated DB:%s, User:%s%n", generatedatabaseName, generatedatabaseUser);

                        // Insert registry data
                        try (PreparedStatement stmt = con.prepareStatement(
                                "INSERT INTO available (uuid, username, password, db) VALUES (?,?,?,?)")) {
                            stmt.setString(1, saveUUID);
                            stmt.setString(2, generatedatabaseUser);
                            stmt.setString(3, generatedatabasePassword);
                            stmt.setString(4, generatedatabaseName);
                            stmt.executeUpdate();
                        }

                        // Assign instance connection fields
                        storeDatabaseName = generatedatabaseName;
                        databaseUrl = "" + storeDatabaseName; //your db url here + storeDatabaseName;
                        databaseUser = generatedatabaseUser;
                        databasePassword = generatedatabasePassword;

                        // Create user database(per machine)
                        try (Statement stmtDb = con.createStatement()) {
                            stmtDb.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + storeDatabaseName + "`");
                            stmtDb.executeUpdate("USE `" + storeDatabaseName + "`");
                        }

                        // Create MySQL user and grant it for user & admin access
                        try (Statement stmt = con.createStatement()) {
                            stmt.executeUpdate("CREATE USER IF NOT EXISTS '" + generatedatabaseUser +
                                    "'@'%' IDENTIFIED BY '" + generatedatabasePassword + "'");
                            stmt.executeUpdate("GRANT SELECT, INSERT, UPDATE, CREATE, ALTER ON `" + storeDatabaseName +
                                    "`.* TO '" + generatedatabaseUser + "'@'%'");
                            stmt.executeUpdate("GRANT SELECT, INSERT, UPDATE, CREATE, ALTER " +
                                    "ON `" + storeDatabaseName + "`.* TO ''@'%'"); // db admin access
                            stmt.executeUpdate("FLUSH PRIVILEGES");
                        }

                        // Ensure data table exists
                        try (Statement stmtTable = con.createStatement()) {
                            String createUserTableSQL = """
                        CREATE TABLE IF NOT EXISTS data (
                          id INT NOT NULL AUTO_INCREMENT,
                          keyword VARCHAR(255),
                          times VARCHAR(255),  -- now text-based
                          link TEXT,
                          link_domain TEXT,
                          link_default TEXT,
                          found BOOLEAN DEFAULT FALSE,
                          crawl_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY(id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """;

                            stmtTable.executeUpdate(createUserTableSQL);
                        }
                    }

                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    /**
     * Using PowerShell, get the machine UUID
     * @return the machine UUID, or empty string on error
     */
    public String getSaveUUID() {
        try {
            uuidProcess = POWFORUUID.start();
            try (BufferedReader pwReader = new BufferedReader(new InputStreamReader(uuidProcess.getInputStream()))) {
                String lineChecking;
                while ((lineChecking = pwReader.readLine()) != null) {
                    lineChecking = lineChecking.trim();
                    if (lineChecking.isEmpty()) continue;
                    if (lineChecking.equalsIgnoreCase("UUID")) continue;
                    if (lineChecking.endsWith("---")) continue;
                    saveUUID = lineChecking;
                    break;
                }

                int psExitCode = uuidProcess.waitFor();
                if (psExitCode == 0 && !saveUUID.isEmpty()) {
                    readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: UUID (PowerShell): " + saveUUID);
                    return saveUUID;
                }
            }
            readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: UUID fetch failed (WMIC & PowerShell)");

        } catch (IOException | InterruptedException ex) {
            readInfo.logs.add(readInfo.timestamp() + "DatabaseConnection: getSaveUUID error: " + ex.getMessage());
            ex.printStackTrace();
        }
        return saveUUID;
    }

    /**
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return random number between min and max
     */
    private int getRandomNumber(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    /**
     * @return the registered email for this UUID, or null if not found
     */
    public String fetchEmail() {
        try {
            final String ADMIN_DB_URL = ROOT_DATABASE_URL + "webcrawler";
            try (Connection con = DriverManager.getConnection(ADMIN_DB_URL, ROOT_USER, ROOT_PASSWORD);
                 PreparedStatement ps = con.prepareStatement("SELECT email FROM available WHERE uuid = ? LIMIT 1")) {
                ps.setString(1, saveUUID);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("email") : null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
