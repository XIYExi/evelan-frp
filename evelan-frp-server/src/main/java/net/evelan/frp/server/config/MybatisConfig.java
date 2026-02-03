package net.evelan.frp.server.config;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

public class MybatisConfig {
    private static SqlSessionFactory sessionFactory;

    static {
        try {
            initSqlSessionFactory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initSqlSessionFactory() throws IOException {
        DataSource dataSource = createDruidDataSource();
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment env = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(env);
        sessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }


    private static DataSource createDruidDataSource() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.h2.Driver");
        properties.setProperty("username", "sa");
        properties.setProperty("password", "");
        properties.setProperty("url", "jdbc:h2:mem:evelan_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        DruidDataSourceFactory druidDataSourceFactory = new DruidDataSourceFactory();
        druidDataSourceFactory.setProperties(properties);
        return druidDataSourceFactory.getDataSource();
    }

}
