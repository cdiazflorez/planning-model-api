package com.mercadolibre.planning.model.api.config;

import com.fury.api.FuryUtils;
import com.fury.api.exceptions.FuryDecryptException;
import com.fury.api.exceptions.FuryNotFoundAPPException;
import com.fury.api.exceptions.FuryUpdateException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@ConfigurationProperties(prefix = "spring.datasource")
@Configuration
@Data
@SuppressFBWarnings(
        value = "USFW_UNSYNCHRONIZED_SINGLETON_FIELD_WRITES",
        justification = "Spring handles this assignation"
)
public class DataSourceConfig {

    private String url;
    private String username;
    private String password;
    private String driverClassName;

    @Bean
    @Profile({"!development"})
    public DataSource dataSource()
            throws FuryDecryptException, FuryNotFoundAPPException, FuryUpdateException {
        return buildDataSource(FuryUtils.getEnv(password));
    }

    @Bean
    @Profile({"development"})
    public DataSource localDataSource() {
        return buildDataSource(getPassword());
    }

    private DataSource buildDataSource(final String password) {
        return DataSourceBuilder.create()
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .url(url)
                .build();
    }
}
