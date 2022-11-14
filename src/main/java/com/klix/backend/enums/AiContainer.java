package com.klix.backend.enums;


import javax.annotation.PostConstruct;

import com.klix.backend.config.NetworkProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


/**
 * holds all referenced Ai-Docker-Containers
 */
@Slf4j
public enum AiContainer
{
    // container choices
    CASCADE("klix-ai-cascade-localisation-url"),
    CNN("klix-ai-cnn-localisation-url"),
    FACENET("klix-ai-facenet-localisation-url");

    private static NetworkProperties networkProperties;

    private String containerUrl;

    /**
     * This inner class provides access to NetworkProperties.
     * It exists because it is impossible to autowire anything in an Enum directly.
     */
    @Component
    public static class NetworkPropertiesInjector
    {
        @Autowired private NetworkProperties np;

        @PostConstruct
        public void postConstruct() {
            AiContainer.setNetworkProperties(np);
        }
    }

    /**
     * Associates a container-url key to the application.yml file to the AiContainer instance created.
     * @param containerUrl  the key
     */
    AiContainer(String containerUrl)
    {
        this.containerUrl = containerUrl;
    }

    /**
     * return the corresponding container url for enum instances
     */
    public String getUrl()
    {
        if (networkProperties == null)
        {
            log.warn("networkProperties is null. Exit.");
            return null;
        }

        return this.containerUrl != null ? networkProperties.getNetwork().get(this.containerUrl) : null;
    }

    /**
     * one-time setter to assign NetworkProperties by inner class
     */
    private static void setNetworkProperties(NetworkProperties np)
    {
        networkProperties = np;
    }
}
