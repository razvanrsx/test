package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public class GatewayProperties {

    private ServiceEndpoint authorization = new ServiceEndpoint("http://authorization-service:8080");
    private ServiceEndpoint user = new ServiceEndpoint("http://user-service:8081");
    private ServiceEndpoint device = new ServiceEndpoint("http://device-service:8082");

    public ServiceEndpoint getAuthorization() {
        return authorization;
    }

    public void setAuthorization(ServiceEndpoint authorization) {
        this.authorization = authorization;
    }

    public ServiceEndpoint getUser() {
        return user;
    }

    public void setUser(ServiceEndpoint user) {
        this.user = user;
    }

    public ServiceEndpoint getDevice() {
        return device;
    }

    public void setDevice(ServiceEndpoint device) {
        this.device = device;
    }

    public static class ServiceEndpoint {
        private String url;

        public ServiceEndpoint() {
        }

        public ServiceEndpoint(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
