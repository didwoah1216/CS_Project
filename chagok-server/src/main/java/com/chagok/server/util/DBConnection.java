package com.chagok.server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL JDBC 드라이버가 없습니다", ex);
        }
    }

    private DBConnection() {
    }

    public static Connection get() {
        ServerConfig config = ServerConfig.get();
        try {
            return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }
}
