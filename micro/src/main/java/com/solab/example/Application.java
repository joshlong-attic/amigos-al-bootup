package com.solab.example;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import io.micronaut.runtime.Micronaut;

/**
 * Spring Boot entry point.
 *
 * @author Enrique Zamudio
 *         Date: 3/13/17 7:07 PM
 */
@Factory
public class Application {

    @Bean @Context
    public AsyncServer protoServer(Lookups handler, Environment env) {
        var server = new AsyncServer(
                env.getProperty("proto.port", Integer.class, 8001),
                env.getProperty("proto.threads", Integer.class,
                        Runtime.getRuntime().availableProcessors()),
                handler);
        server.run();
        return server;
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
