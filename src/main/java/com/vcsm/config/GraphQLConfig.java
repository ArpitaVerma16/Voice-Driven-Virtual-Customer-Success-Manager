package com.vcsm.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(GraphQLScalarType.newScalar()
                .name("LocalDateTime")
                .description("Custom LocalDateTime scalar")
                .coercing(new LocalDateTimeCoercing())
                .build());
    }

    // Custom LocalDateTime coercing
    private static class LocalDateTimeCoercing implements graphql.schema.Coercing<LocalDateTime, String> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public String serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof LocalDateTime) {
                return ((LocalDateTime) dataFetcherResult).format(FORMATTER);
            }
            return null;
        }

        @Override
        public LocalDateTime parseValue(Object input) {
            if (input instanceof String) {
                return LocalDateTime.parse((String) input, FORMATTER);
            }
            return null;
        }

        @Override
        public LocalDateTime parseLiteral(Object input) {
            if (input instanceof String) {
                return LocalDateTime.parse((String) input, FORMATTER);
            }
            return null;
        }
    }
}