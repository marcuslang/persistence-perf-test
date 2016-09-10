package org.rapidpm.service.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UsdaDAO {

  private HikariDataSource dataSource;

  @PostConstruct
  private void init() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:postgresql://10.1.1.200:5432/testdb");
    config.setUsername("dbuser");
    config.setPassword("dbuser");
    dataSource = new HikariDataSource(config);
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public int getCountOfFoodGroups() throws SQLException {
    final ResultSet resultSet = executeStatement("select count(*) from fd_group");
    resultSet.next();
    final int count = resultSet.getInt(1);
    resultSet.close();
    return count;
  }

  private ResultSet executeStatement(String sql) throws SQLException {
    final Statement statement = dataSource.getConnection().createStatement();
    statement.execute(sql);
    return statement.getResultSet();
  }


}
