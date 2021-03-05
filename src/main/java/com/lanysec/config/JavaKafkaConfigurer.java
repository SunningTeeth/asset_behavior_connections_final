package com.lanysec.config;

import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author daijb
 * @date 2021/3/5 21:31
 */
public class JavaKafkaConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(JavaKafkaConfigurer.class);

    private static volatile Properties properties;

    public static Properties getKafkaProperties(String[] args) {
        if (null == properties) {
            synchronized (JavaKafkaConfigurer.class) {
                if (properties == null) {
                    if (args == null || args.length <= 0) {
                        //获取配置文件kafka.properties的内容
                        Properties kafkaProperties = new Properties();
                        try {
                            kafkaProperties.load(JavaKafkaConfigurer.class.getClassLoader().getResourceAsStream("kafka.properties"));
                        } catch (Throwable throwable) {
                            logger.error("load kafka configurer failed due to ", throwable);
                        }
                        properties = kafkaProperties;
                    } else {
                        ParameterTool parameters = ParameterTool.fromArgs(args);
                        properties = parameters.getProperties();
                    }

                }
            }
        }
        return properties;
    }

    public static void configureSecurityAuthLoginConfig() {
        //如果用-D或者其它方式设置过，这里不再设置
        if (null == System.getProperty("java.security.auth.login.config")) {
            //请注意将XXX修改为自己的路径
            //这个路径必须是一个文件系统可读的路径，不能被打包到jar中
            System.setProperty("java.security.auth.login.config", getKafkaProperties(null).getProperty("java.security.auth.login.config"));
        }
    }
}
