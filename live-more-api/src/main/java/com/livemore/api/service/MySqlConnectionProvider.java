package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class MySqlConnectionProvider {

    private final AppProperties appProperties;

    public MySqlConnectionProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Connection openConnection() throws SQLException {
        AppProperties.Mysql mysql = appProperties.getPersistence().getMysql();
        if (isBlank(mysql.getUrl()) || isBlank(mysql.getUser())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_not_configured");
        }
        return DriverManager.getConnection(mysql.getUrl(), mysql.getUser(), mysql.getPassword());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
