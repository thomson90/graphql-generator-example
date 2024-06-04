package de.myCompany.myProject.configurations;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitlabConfig {

  @Value(value = "${graphql.endpoint.url}")
  private String graphqlUrl;

  @Value(value = "${graphql.endpoint.token:}")
  private String graphqlToken;

  /**
   * The Spring reactive {@link WebClient} that will execute the HTTP requests for GraphQL queries and mutations.<BR/>
   */
  @Bean
  @Primary
  public WebClient webClient() {
    return WebClient.builder()
             .baseUrl(graphqlUrl)
             .defaultHeader("Content-Type", "application/json")
             .defaultHeader("Authorization", "Bearer " + graphqlToken)
             .defaultUriVariables(Collections.singletonMap("url", graphqlUrl))
             .build();
  }
}
