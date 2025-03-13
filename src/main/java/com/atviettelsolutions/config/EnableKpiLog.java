package com.atviettelsolutions.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(KpiLogConfigurationSelector.class)
@Documented
public @interface EnableKpiLog {
}
