package com.ultramega.refinedflowanalytics;

import com.ultramega.refinedflowanalytics.config.ClientConfig;
import com.ultramega.refinedflowanalytics.config.ServerConfig;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class Platform {
    @Nullable
    private static Supplier<ClientConfig> clientConfigProvider;
    @Nullable
    private static Supplier<ServerConfig> serverConfigProvider;

    private Platform() {
    }

    public static void setClientConfigProvider(final Supplier<ClientConfig> provider) {
        clientConfigProvider = provider;
    }

    public static ClientConfig getClientConfig() {
        return requireNonNull(clientConfigProvider, "Client config is not loaded yet").get();
    }

    public static void setServerConfigProvider(final Supplier<ServerConfig> provider) {
        serverConfigProvider = provider;
    }

    public static ServerConfig getServerConfig() {
        return requireNonNull(serverConfigProvider, "Server config is not loaded yet").get();
    }
}
