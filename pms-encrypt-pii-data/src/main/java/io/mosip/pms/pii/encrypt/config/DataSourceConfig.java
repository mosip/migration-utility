package io.mosip.pms.pii.encrypt.config;

import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig extends HibernateDaoConfig {

    @Override
    public java.util.Map<String, Object> jpaProperties() {
        return super.jpaProperties();
    }
} 