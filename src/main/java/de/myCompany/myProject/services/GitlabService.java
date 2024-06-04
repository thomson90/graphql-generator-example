package de.myCompany.myProject.services;

import com.graphql_java_generator.client.request.Builder;
import com.graphql_java_generator.client.request.ObjectResponse;
import com.graphql_java_generator.exception.GraphQLRequestExecutionException;
import com.graphql_java_generator.exception.GraphQLRequestPreparationException;
import de.myCompany.myProject.gitlab.CommitCreatePayload;
import de.myCompany.myProject.gitlab.CreateBranchPayload;
import de.myCompany.myProject.gitlab.MergeRequest;
import de.myCompany.myProject.gitlab.EchoCreatePayload;
import de.myCompany.myProject.gitlab.MergeRequestConnection;
import de.myCompany.myProject.gitlab.MergeRequestCreatePayload;
import de.myCompany.myProject.gitlab.Mutation;
import de.myCompany.myProject.gitlab.Project;
import de.myCompany.myProject.gitlab.Query;
import de.myCompany.myProject.gitlab.util.GraphQLRequest;
import de.myCompany.myProject.gitlab.util.MutationExecutor;
import de.myCompany.myProject.gitlab.util.QueryExecutor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@SuppressWarnings({"UnusedReturnValue", "unused"})
class GitlabService {

  private static final String ERROR_EMPTY_PARAMETER = "Parameter '%s' must not be null or empty!";

  protected static final String RANDOM_MESSAGE = UUID.randomUUID().toString();

  private final QueryExecutor queryExecutor;

  private final MutationExecutor mutationExecutor;

  public GitlabService(QueryExecutor queryExecutor, MutationExecutor mutationExecutor) {
    this.queryExecutor = queryExecutor;
    this.mutationExecutor = mutationExecutor;
  }

  public void requireAccess() {
    canCallQuery();
    canCallMutation();
  }

  public GitlabResult createBranch(String projectPath, String baseBranch, String branchName) {
    checkArgument(isNotBlank(projectPath), ERROR_EMPTY_PARAMETER, "projectPath");
    checkArgument(isNotBlank(baseBranch), ERROR_EMPTY_PARAMETER, "baseBranch");
    checkArgument(isNotBlank(branchName), ERROR_EMPTY_PARAMETER, "branchName");

    Mutation mutationResponse = callMutation(
      "mutation CREATE_BRANCH($projectPath: ID!, $sourceBranch: String!, $targetBranch: String!) {" +
        "  createBranch(" +
        "    input: {projectPath: $projectPath, name: $sourceBranch, ref: $targetBranch}" +
        "  ) {" +
        "    errors" +
        "  }" +
        "}",
      Map.of(
        "projectPath", projectPath,
        "sourceBranch", branchName,
        "targetBranch", baseBranch
      ));

    // note: wrong token just returns NULL (no exception)
    return new GitlabResult(
      ofNullable(mutationResponse.getCreateBranch())
        .map(CreateBranchPayload::getErrors)
        .orElse(List.of())
    );
  }

  public GitlabResult commit(String projectPath, String branchName, String fileName, String fileContent, boolean mustBeCreated, String createMessage, String updateMessage) {
    checkArgument(isNotBlank(projectPath), ERROR_EMPTY_PARAMETER, "projectPath");
    checkArgument(isNotBlank(branchName), ERROR_EMPTY_PARAMETER, "branchName");
    checkArgument(isNotBlank(fileName), ERROR_EMPTY_PARAMETER, "fileName");
    checkArgument(isNotBlank(fileContent), ERROR_EMPTY_PARAMETER, "fileContent");
    checkArgument(isNotBlank(createMessage), ERROR_EMPTY_PARAMETER, "createMessage");
    checkArgument(isNotBlank(updateMessage), ERROR_EMPTY_PARAMETER, "updateMessage");

    // hint: create followed by update
    // * if the file cannot be created (in creation mode) it will always be updated
    // * this might be the case if creation is called twice (by same or different users)
    Mutation mutationResponse = callMutation(
      "mutation CREATE_FILE($projectPath: ID!, $sourceBranch: String!, $createMessage: String!, $updateMessage: String!, $filePath: String!, $fileContent: String!, $create: Boolean!) {" +
        "  create: commitCreate(" +
        "    input: {projectPath: $projectPath, branch: $sourceBranch, message: $createMessage, actions: [{action: CREATE, filePath: $filePath}]}" +
        "  ) @include (if: $create) {" +
        "    errors" +
        "  }" +
        "  commitCreate(" +
        "    input: {projectPath: $projectPath, branch: $sourceBranch, message: $updateMessage, actions: [{action: UPDATE, filePath: $filePath, content: $fileContent}]}" +
        "  ) {" +
        "    errors" +
        "  }" +
        "}",
      Map.of(
        "projectPath", projectPath,
        "sourceBranch", branchName,
        "createMessage", createMessage,
        "updateMessage", updateMessage,
        "filePath", fileName,
        "fileContent", fileContent,
        "create", mustBeCreated
      ));

    // note: wrong token just returns NULL (no exception)
    return new GitlabResult(
      ofNullable(mutationResponse.getCommitCreate())
        .map(CommitCreatePayload::getErrors)
        .orElse(List.of())
    );
  }

  public GitlabResult createMergeRequest(String projectPath, String sourceBranch, String baseBranch, String commitMessage) {
    checkArgument(isNotBlank(projectPath), ERROR_EMPTY_PARAMETER, "projectPath");
    checkArgument(isNotBlank(sourceBranch), ERROR_EMPTY_PARAMETER, "sourceBranch");
    checkArgument(isNotBlank(commitMessage), ERROR_EMPTY_PARAMETER, "commitMessage");
    checkArgument(isNotBlank(baseBranch), ERROR_EMPTY_PARAMETER, "baseBranch");

    Mutation mutationResponse = callMutation(
      "mutation CREATE_MERGE($projectPath: ID!, $sourceBranch: String!, $targetBranch: String!, $commitMessage: String!) {" +
        "  mergeRequestCreate(" +
        "    input: {projectPath: $projectPath, title: $commitMessage, sourceBranch: $sourceBranch, targetBranch: $targetBranch}" +
        "  ) {" +
        "    errors" +
        "  }" +
        "}",
      Map.of(
        "projectPath", projectPath,
        "sourceBranch", sourceBranch,
        "targetBranch", baseBranch,
        "commitMessage", commitMessage
      ));

    Query queryResponse = callQuery(
      "query OPEN_MERGE_REQUESTS($projectPath: ID!, $sourceBranch: String!) {" +
        "  project(fullPath: $projectPath) {" +
        "    mergeRequests(state: opened, sourceBranches: [$sourceBranch], first: 1) {" +
        "      nodes {" +
        "        webUrl" +
        "      }" +
        "    }" +
        "  }" +
        "}",
      Map.of(
        "projectPath", projectPath,
        "sourceBranch", sourceBranch
      ));

    // note: wrong token just returns NULL (no exception)
    return new GitlabResult(
      ofNullable(mutationResponse.getMergeRequestCreate())
        .map(MergeRequestCreatePayload::getErrors)
        .orElse(List.of()),
      ofNullable(queryResponse.getProject())
        .map(Project::getMergeRequests)
        .map(MergeRequestConnection::getNodes)
        .orElse(List.of())
        .stream()
        .findFirst()
        .map(MergeRequest::getWebUrl)
        .orElseThrow(
          () -> new IllegalStateException("WebUrl not found in Gitlab response!")
        )
    );
  }

  protected void canCallQuery() {
    Query queryResponse = callQuery(
      "query ECHO($message: String!) {" +
        "  echo(text: $message)" +
        "}",
      Map.of(
        "message", RANDOM_MESSAGE
      ));

    // note: wrong token just returns NULL (no exception)
    ofNullable(queryResponse.getEcho())
      .filter(msg -> msg.endsWith(RANDOM_MESSAGE))
      .orElseThrow(
        () -> new IllegalStateException("Current user has no READ ACCESS to Gitlab GraphQL service!")
      );
  }

  protected void canCallMutation() {
    Mutation mutationResponse = callMutation(
      "mutation ECHO($message: String!) {" +
        "  echoCreate(input: {errors: [], messages: [$message]}) {" +
        "    echoes" +
        "  }" +
        "}",
      Map.of(
        "message", RANDOM_MESSAGE
      ));

    // note: wrong token just returns NULL (no exception)
    ofNullable(mutationResponse.getEchoCreate())
      .map(EchoCreatePayload::getEchoes)
      .orElse(List.of())
      .stream()
      .findFirst()
      .filter(msg -> msg.endsWith(RANDOM_MESSAGE))
      .orElseThrow(
        () -> new IllegalStateException("Current user has no WRITE ACCESS to Gitlab GraphQL service!")
      );
  }

  @SuppressWarnings("SameParameterValue")
  private Query callQuery(String queryResponseDef, Map<String, Object> parameters) {
    try {
      return queryExecutor.execWithBindValues(getObjectResponse(queryResponseDef), parameters);
    } catch (WebClientRequestException | GraphQLRequestExecutionException | GraphQLRequestPreparationException cause) {
      throw new IllegalStateException("Gitlab GraphQL service not available!", cause);
    }
  }

  private Mutation callMutation(String queryResponseDef, Map<String, Object> parameters) {
    try {
      return mutationExecutor.execWithBindValues(getObjectResponse(queryResponseDef), parameters);
    } catch (WebClientRequestException | GraphQLRequestExecutionException | GraphQLRequestPreparationException cause) {
      throw new IllegalStateException("Gitlab GraphQL service not available!", cause);
    }
  }

  private ObjectResponse getObjectResponse(String queryResponseDef) throws GraphQLRequestPreparationException {
    return new Builder(GraphQLRequest.class).withQueryResponseDef(queryResponseDef).build();
  }

  static class GitlabResult {
    @NotNull
    private final List<String> errors;

    private final String webUrl;

    public GitlabResult(List<String> errors) {
      this(errors, "");
    }

    public GitlabResult(List<String> errors, String webUrl) {
      this.errors = requireNonNull(errors);
      this.webUrl = requireNonNull(webUrl);
    }

    public List<String> getErrors() {
      return errors;
    }

    public String getWebUrl() {
      return webUrl;
    }

    public boolean successful() {
      return errors.isEmpty();
    }
  }
}
