package org.saw.webcrawler.corefeatures;

import org.openqa.selenium.json.JsonOutput;
import org.saw.webcrawler.fxfeatures.DatabaseConnection;
import org.saw.webcrawler.fxfeatures.CloudflaredConnection;
import org.saw.webcrawler.fxfeatures.readInfo;

import java.sql.*;


public class DbReadWrite {
    private DatabaseConnection databaseConnection = new DatabaseConnection();

    public void saveDatabase(String keyword, int times, String link, String domainLink, String defaultLink, byte[] imageData, boolean found) {
        if (!checkingBoth()) {
            System.out.println("Tunnel or DB not ready, cannot save");
            return;
        }
        String sql = "INSERT INTO data (keyword, times, link, link_domain, link_default, image, found) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection con = DriverManager.getConnection(
                    databaseConnection.getDatabaseUrl(),
                    databaseConnection.getDatabaseUser(),
                    databaseConnection.getDatabasePassword());
                 PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, keyword);
                pstmt.setInt(2, times);
                pstmt.setString(3, link);
                pstmt.setString(4, domainLink);
                pstmt.setString(5, defaultLink);
                if (imageData != null) {
                    pstmt.setBytes(6, imageData);
                } else {
                    pstmt.setNull(6, java.sql.Types.BLOB);
                }
                pstmt.setBoolean(7, found);
                pstmt.executeUpdate();
                System.out.println("save" +  databaseConnection.getDatabaseUrl());
                System.out.println("save" +  databaseConnection.getDatabaseUser());
                System.out.println("save" +  databaseConnection.getDatabasePassword());
//                pstmt.close();
//                con.close();
                System.out.println("Data Successfully Inserted");
            }catch (SQLException e) {
                System.out.println(readInfo.timestamp()  + " DbReadWrite file: SQL Error: at saveDatabase: " + e.getMessage());
                readInfo.logs.add(readInfo.timestamp() + " DbReadWrite file: SQL Error: at saveDatabase: " + e.getMessage());
            }
        }

    public ResultSet readDatabase(String keyword, String url) {
        if (!checkingBoth()) {
            System.out.println("Tunnel or DB not ready, cannot read");
            return null;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT keyword, times, link, link_default, crawl_time FROM data WHERE DATE(crawl_time) = CURDATE()"
        );

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND keyword LIKE ?");
        }
        if (url != null && !url.isEmpty()) {
            sql.append(" AND link_default LIKE ?");
        }

        try {
            Connection con = DriverManager.getConnection(
                    databaseConnection.getDatabaseUrl(),
                    databaseConnection.getDatabaseUser(),
                    databaseConnection.getDatabasePassword());
            PreparedStatement pstmt = con.prepareStatement(sql.toString());

            int idx = 1;
            if (keyword != null && !keyword.isEmpty()) {
                pstmt.setString(idx++, "%" + keyword + "%");
            }
            if (url != null && !url.isEmpty()) {
                pstmt.setString(idx, "%" + url + "%");
            }

            return pstmt.executeQuery(); // ⚠ caller must consume immediately

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public ResultSet searchDatabase(String searchText, String month) {
        if (!checkingBoth()) {
            System.out.println("Database or tunnel not ready");
            return null;
        }

        try {
            Connection con = DriverManager.getConnection(
                    databaseConnection.getDatabaseUrl(),
                    databaseConnection.getDatabaseUser(),
                    databaseConnection.getDatabasePassword());

            StringBuilder sql = new StringBuilder(
                    "SELECT keyword, times, link, link_domain, found, crawl_time " +
                            "FROM data WHERE 1=1"
            );
//WHERE DATE(crawl_time) = CURDATE()
            // month filter
            if (month != null && !month.isEmpty()) {
                sql.append(" AND MONTHNAME(crawl_time) = ?");
            }

            // one search field matches keyword OR link OR domain
            if (searchText != null && !searchText.isEmpty()) {
                sql.append(" AND (keyword LIKE ? OR link LIKE ? OR link_domain LIKE ?)");
            }

            PreparedStatement pstmt = con.prepareStatement(sql.toString());

            int idx = 1;
            if (month != null && !month.isEmpty()) {
                pstmt.setString(idx++, month);
            }

            if (searchText != null && !searchText.isEmpty()) {
                String like = "%" + searchText + "%";
                pstmt.setString(idx++, like); // keyword
                pstmt.setString(idx++, like); // link
                pstmt.setString(idx++, like); // domain
            }

            System.out.println("SQL: " + pstmt); // debug
            return pstmt.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    private boolean checkingBoth(){
            if (CloudflaredConnection.tunnelExists() && databaseConnection.checkingUser()) {
                return true;
            }
            return false;
        }

        }

