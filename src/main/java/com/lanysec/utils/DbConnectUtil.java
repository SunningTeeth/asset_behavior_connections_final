package com.lanysec.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author daijb
 * @date 2021/3/5 21:40
 */
public class DbConnectUtil {

    private static final Logger logger = LoggerFactory.getLogger(DbConnectUtil.class);

    /**
     * 创建连接池对象
     */
    private static ComboPooledDataSource dataSource = null;

    static {
        dataSourceConfigurer();
    }

    /**
     * 配置连接池
     */
    private static void dataSourceConfigurer() {
        try {
            //设置数据源
            dataSource = new ComboPooledDataSource();
            dataSource.setDriverClass("com.mysql.jdbc.Driver");
            String addr = SystemUtil.getHostIp();
            String username = SystemUtil.getMysqlUser();
            String password = SystemUtil.getMysqlPassword();
            String url = "jdbc:mysql://" + addr + ":3306/csp?useEncoding=true&characterEncoding=utf-8&serverTimezone=UTC";
            dataSource.setJdbcUrl(url);
            dataSource.setUser(username);
            dataSource.setPassword(password);
            dataSource.setMaxIdleTime(60);
            dataSource.setIdleConnectionTestPeriod(60);
            logger.info("load dataSource url :" + url + " user :" + username + "/" + password);
        } catch (Exception e) {
            logger.error("get database connection failed due to ", e);
        }
    }

    public static Connection getConnection() {
        Connection conn = null;
        if (dataSource == null) {
            dataSourceConfigurer();
        }
        try {
            conn = dataSource.getConnection();
        } catch (Throwable throwable) {
            logger.error("get database connection failed ", throwable);
        }
        return conn;
    }

    /**
     * 执行一个任务
     *
     * @param sql    sql
     * @param params PrepareStatement 参数
     */
    public static void execUpdateTask(String sql, String... params) {
        Connection connection = getConnection();
        try {
            if (connection == null) {
                logger.error("mysql connection is null.");
                return;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            for (int i = 1; i <= params.length; i++) {
                ps.setString(i, params[i - 1]);
            }
            ps.execute();
        } catch (Throwable throwable) {
            logger.error("execute update task failed ", throwable);
        }

    }

    /**
     * 提交事物
     */
    public static void commit(Connection conn) {
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                logger.error("提交事物失败,Connection: " + conn, e);
                close(conn);
            }
        }
    }

    /**
     * 事物回滚
     *
     * @deprecated
     */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                logger.error("事物回滚失败,Connection:" + conn, e);
                close(conn);
            }
        }
    }

    /**
     * 关闭连接
     */
    private static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("关闭连接失败,Connection:" + conn);
            }
        }
    }
}
