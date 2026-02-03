package net.evelan.frp.server.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

import java.util.Properties;

public class DruidDataSourceFactory extends UnpooledDataSourceFactory {
    public DruidDataSourceFactory() {
        this.dataSource = new DruidDataSource();
    }

    @Override
    public void setProperties(Properties properties) {
        try {
            DruidDataSource druidDataSource = (DruidDataSource) this.dataSource;
            druidDataSource.setUrl(properties.getProperty("url"));
            druidDataSource.setUsername(properties.getProperty("username"));
            druidDataSource.setPassword(properties.getProperty("password"));
            druidDataSource.setDriverClassName(properties.getProperty("driverClassName"));

            // 连接池优化配置
            druidDataSource.setInitialSize(5);
            druidDataSource.setMinIdle(5);
            druidDataSource.setMaxActive(20);
            druidDataSource.setMaxWait(60000);
            druidDataSource.setTimeBetweenEvictionRunsMillis(60000);
            druidDataSource.setMinEvictableIdleTimeMillis(300000);
            druidDataSource.setTestWhileIdle(true);
            druidDataSource.setTestOnBorrow(false);
            druidDataSource.setTestOnReturn(false);
            druidDataSource.setValidationQuery("SELECT 1");
            druidDataSource.init();
        } catch (Exception e) {

        }

    }

}
