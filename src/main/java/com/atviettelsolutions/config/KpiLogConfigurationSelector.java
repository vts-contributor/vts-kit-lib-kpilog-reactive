package com.atviettelsolutions.config;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

public class KpiLogConfigurationSelector implements ImportSelector, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        List<String> imports = new ArrayList<>();
        imports.add(KpiLogAutoConfiguration.class.getName());
        imports.add(KpiLogRestConfiguration.class.getName());
        boolean grpcEnabled = environment.getProperty("kpilog.grpc.enable", Boolean.class, false);
        if (grpcEnabled) {
            imports.add(KpiLogGrpcConfiguration.class.getName());
        }
        return imports.toArray(new String[0]);
    }
}
