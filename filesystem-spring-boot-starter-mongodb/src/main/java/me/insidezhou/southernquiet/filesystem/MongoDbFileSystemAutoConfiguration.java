package me.insidezhou.southernquiet.filesystem;

import me.insidezhou.southernquiet.filesystem.driver.MongoDbFileSystem;
import com.mongodb.gridfs.GridFS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties
public class MongoDbFileSystemAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MongoDbFileSystem mongoDbFileSystem(Properties properties, MongoOperations mongoOperations, GridFsOperations gridFsOperations, GridFS gridFS) {
        return new MongoDbFileSystem(properties, mongoOperations, gridFsOperations, gridFS);
    }

    @Bean
    @ConditionalOnMissingBean
    public GridFS gridFS(MongoDbFactory factory) {
        return new GridFS(factory.getLegacyDb());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.file-system.mongodb")
    public Properties mongodbFileSystemProperties() {
        return new Properties();
    }

    /**
     * @see org.springframework.boot.autoconfigure.mongo.MongoProperties
     */
    public static class Properties {
        /**
         * 路径集合
         */
        private String pathCollection = "PATH";
        /**
         * 文件大小阈值，大于该阈值的使用GridFs而不是普通Document。阈值上限是mongodb上限16m。
         */
        private Integer fileSizeThreshold = 15 * 1024 * 1024;

        public Integer getFileSizeThreshold() {
            return fileSizeThreshold;
        }

        public void setFileSizeThreshold(Integer fileSizeThreshold) {
            this.fileSizeThreshold = fileSizeThreshold;
        }

        public String getPathCollection() {
            return pathCollection;
        }

        public void setPathCollection(String pathCollection) {
            this.pathCollection = pathCollection;
        }
    }
}
