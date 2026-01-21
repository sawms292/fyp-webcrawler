package org.saw.webcrawler.corefeatures;

import org.saw.webcrawler.fxfeatures.DatabaseConnection;
import org.saw.webcrawler.fxfeatures.readInfo;

import java.sql.*;

/**
 * Provides database read/write operations for crawler results stored in MySQL
 */
public class DbReadWrite {
    /**
     * connection prepare to supply db connection info
     */
    private final DatabaseConnection databaseConnection = new DatabaseConnection();

    /**
     * Constructs a DbReadWrite instance and ensures the database is initialized
     */
    public DbReadWrite() {
        boolean ok = databaseConnection.checkingUser();
        if (!ok) {
            throw new IllegalStateException("Database not ready: checkingUser() returned false");
        }
    }

    /**
     * @param keyword the keyword searched
     * @param times the number of times the keyword was found
     * @param link the raw link extracted from the website
     * @param domainLink extracted domain link based on url provided by user
     * @param defaultLink default link insert by user
     * @param found whether the keyword was found or not
     */
    public void saveDatabase(String keyword,
                             String times,
                             String link,
                             String domainLink,
                             String defaultLink,
                             boolean found) {

        // SQL insert statement
        String sql = "INSERT INTO data (keyword, times, link, link_domain, link_default, found) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        // get connection info
        String url  = databaseConnection.getDatabaseUrl();
        String user = databaseConnection.getDatabaseUser();
        String pass = databaseConnection.getDatabasePassword();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Database URL is empty. Did checkingUser() run?");
        }
        //try insert result to db
        try (Connection con = DriverManager.getConnection(url, user, pass);
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, keyword);
            pstmt.setString(2, times);
            pstmt.setString(3, link);
            pstmt.setString(4, domainLink);
            pstmt.setString(5, defaultLink);
            pstmt.setBoolean(6, found);

            pstmt.executeUpdate();
            //success message
            System.out.println("Data Successfully Inserted");
        } catch (SQLException e) {
            System.out.println(readInfo.timestamp() + " DbReadWrite: SQL Error at saveDatabase: " + e.getMessage());
            readInfo.logs.add(readInfo.timestamp() + " DbReadWrite: SQL Error at saveDatabase: " + e.getMessage());
        }
    }


    /**
     * @param keyword used keyword insert by user
     * @param urlFilter used URL insert by user
     * @param runStart used current tasks crawler start timestamp
     * @return result set of matching records
     */
    public ResultSet readDatabase(String keyword, String urlFilter, Timestamp runStart) {
        // starts a SQL query
        StringBuilder sql = new StringBuilder(
                "SELECT keyword, times, link, link_default, crawl_time " +
                        "FROM data WHERE 1=1"
        );
        // filter by crawl_time if not null
        if (runStart != null) {
            sql.append(" AND crawl_time >= ?");
        }

        // filter by keyword if not null or empty
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND keyword LIKE ?");
        }

        // filter by urlFilter if not null or empty
        if (urlFilter != null && !urlFilter.isEmpty()) {
            sql.append(" AND link_default LIKE ?");
        }

        //new to old
        sql.append(" ORDER BY crawl_time DESC");

        try {
            // get connection info
            String url  = databaseConnection.getDatabaseUrl();
            String user = databaseConnection.getDatabaseUser();
            String pass = databaseConnection.getDatabasePassword();
            // check url db
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("Database URL is empty. Did checkingUser() run?");
            }

            Connection con = DriverManager.getConnection(url, user, pass);
            PreparedStatement pstmt = con.prepareStatement(sql.toString());

            int idx = 1;

            // set parameters for timestamp is not null
            if (runStart != null) {
                pstmt.setTimestamp(idx++, runStart);
            }

            // set parameters for keyword is not null or empty
            if (keyword != null && !keyword.isEmpty()) {
                pstmt.setString(idx++, "%" + keyword + "%");
            }

            // set parameters for urlFilter is not null or empty
            if (urlFilter != null && !urlFilter.isEmpty()) {
                pstmt.setString(idx, "%" + urlFilter + "%");
            }

            //return the executed query
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param searchText text to search in keyword, link, or domain
     * @param month filter by month name (e.g., 'January'); can be null or empty
     * @return result set of matching records
     */
    public ResultSet searchDatabase(String searchText, String month) {
        try {
            // get connection info
            String url  = databaseConnection.getDatabaseUrl();
            String user = databaseConnection.getDatabaseUser();
            String pass = databaseConnection.getDatabasePassword();
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("Database URL is empty. Did checkingUser() run?");
            }

            Connection con = DriverManager.getConnection(url, user, pass);

            // starts a SQL query
            StringBuilder sql = new StringBuilder(
                    "SELECT keyword, times, link, link_domain, found, crawl_time " +
                            "FROM data WHERE 1=1"
            );

            // filter by month if provided
            if (month != null && !month.isEmpty()) {
                sql.append(" AND MONTHNAME(crawl_time) = ?");
            }

            // filter by searchText if provided
            if (searchText != null && !searchText.isEmpty()) {
                sql.append(" AND (keyword LIKE ? OR link LIKE ? OR link_domain LIKE ?)");
            }

            //new to old
            sql.append(" ORDER BY crawl_time DESC");

            PreparedStatement pstmt = con.prepareStatement(sql.toString());
            int idx = 1;

            // set parameters for month filter
            if (month != null && !month.isEmpty()) {
                pstmt.setString(idx++, month);
            }
            // set parameters for searchText filter
            if (searchText != null && !searchText.isEmpty()) {
                String like = "%" + searchText + "%";
                pstmt.setString(idx++, like); // keyword
                pstmt.setString(idx++, like); // link
                pstmt.setString(idx++, like); // domain
            }

            System.out.println("SQL: " + pstmt);
            //return executed query
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
