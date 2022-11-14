package com.klix.backend.config;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;


/**
 * Holds the configureation of application.yml, specifically the one for network:
 */
@Data
@Validated
@ConfigurationProperties
public class NetworkProperties
{
    @NotEmpty
    private Map<String, String> network;
}
