package com.vcsm.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest
public class GraphQLComplaintTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void shouldFetchComplaints() {
        String query = """
            query {
                complaints {
                    content {
                        id
                        description
                        status
                    }
                    pageInfo {
                        totalElements
                        totalPages
                    }
                }
            }
            """;

        graphQlTester.document(query)
            .execute()
            .path("complaints.content")
            .entityList(Object.class)
            .hasSizeGreaterThan(0);
    }

    @Test
    void shouldCreateComplaint() {
        String mutation = """
            mutation {
                createComplaint(input: {
                    description: "Test complaint via GraphQL"
                    category: MAINTENANCE
                    apartmentNumber: "A-101"
                    contactEmail: "test@example.com"
                }) {
                    id
                    description
                    status
                }
            }
            """;

        graphQlTester.document(mutation)
            .execute()
            .path("createComplaint.id")
            .entity(Long.class)
            .isNotNull();
    }

    @Test
    void shouldGetComplaintStats() {
        String query = """
            query {
                complaintStats {
                    total
                    open
                    resolved
                }
            }
            """;

        graphQlTester.document(query)
            .execute()
            .path("complaintStats.total")
            .entity(Long.class)
            .isNotNull();
    }

    @Test
    void shouldSearchWithFilters() {
        String query = """
            query {
                complaints(filter: {
                    status: OPEN
                    category: MAINTENANCE
                    search: "water"
                }) {
                    content {
                        id
                        description
                    }
                }
            }
            """;

        graphQlTester.document(query)
            .execute()
            .path("complaints.content")
            .entityList(Object.class)
            .hasSizeGreaterThanOrEqualTo(0);
    }
}   