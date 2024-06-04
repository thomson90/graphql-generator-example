package de.myCompany.myProject.services;

import com.graphql_java_generator.client.request.ObjectResponse;
import com.graphql_java_generator.exception.GraphQLRequestExecutionException;
import de.myCompany.myProject.gitlab.CommitCreatePayload;
import de.myCompany.myProject.gitlab.CreateBranchPayload;
import de.myCompany.myProject.gitlab.EchoCreateInput;
import de.myCompany.myProject.gitlab.EchoCreatePayload;
import de.myCompany.myProject.gitlab.MergeRequest;
import de.myCompany.myProject.gitlab.MergeRequestConnection;
import de.myCompany.myProject.gitlab.MergeRequestCreatePayload;
import de.myCompany.myProject.gitlab.Mutation;
import de.myCompany.myProject.gitlab.Project;
import de.myCompany.myProject.gitlab.Query;
import de.myCompany.myProject.gitlab.util.MutationExecutor;
import de.myCompany.myProject.gitlab.util.QueryExecutor;
import de.myCompany.myProject.services.GitlabService.GitlabResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static de.myCompany.myProject.configurations.SpringProfiles.LOCAL;
import static de.myCompany.myProject.services.GitlabService.RANDOM_MESSAGE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles(LOCAL)
@MockitoSettings
class GitlabServiceTest {

  private static final String PROJECT_PATH = "projectPath";
  private static final String SOURCE_BRANCH = "sourceBranch";
  private static final String CREATE_MESSAGE = "createMessage";
  private static final String UPDATE_MESSAGE = "updateMessage";
  private static final String TITLE_MESSAGE = "titleMessage";
  private static final String BASE_BRANCH = "baseBranch";
  private static final String FILE_NAME = "fileName";
  private static final String FILE_CONTENT = "fileContent";

  @MockBean
  private QueryExecutor queryExecutor;
  @MockBean
  private MutationExecutor mutationExecutor;
  @Autowired
  private GitlabService gitlabService;

  @BeforeEach
  void setUp() throws Exception {
    doReturn(RANDOM_MESSAGE).when(queryExecutor).echo(anyString(), anyString());
    doReturn(
      EchoCreatePayload.builder()
        .withEchoes(List.of(RANDOM_MESSAGE))
        .build())
      .when(mutationExecutor).echoCreate(anyString(), any(EchoCreateInput.class));
  }

  @Test
  void shouldThrowException_whenRequireAccess_ifGitlabNotReachable() throws Exception {
    doThrow(WebClientRequestException.class)
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.requireAccess());

    verify(queryExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenRequireAccess_ifQueryImpossible() throws Exception {
    doReturn(
      Query.builder().withEcho(
        null
      ).build())
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.requireAccess());

    verify(queryExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenRequireAccess_ifQueryHasWrongResult() throws Exception {
    doReturn(
      Query.builder().withEcho(
        "WrongResult"
      ).build())
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.requireAccess());

    verify(queryExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());

    doReturn("WrongResult").when(queryExecutor).echo(anyString(), anyString());
  }

  @Test
  void shouldThrowException_whenRequireAccess_ifMutationImpossible() throws Exception {
    doReturn(
      Query.builder().withEcho(
        RANDOM_MESSAGE
      ).build())
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());
    doReturn(
      Mutation.builder().withEchoCreate(
        EchoCreatePayload.builder().build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.requireAccess());

    verify(queryExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
    verify(mutationExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenRequireAccess_ifMutationHasWrongResult() throws Exception {
    doReturn(
      Query.builder().withEcho(
        RANDOM_MESSAGE
      ).build())
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());
    doReturn(
      Mutation.builder().withEchoCreate(
        EchoCreatePayload.builder().withEchoes(
          List.of("WrongResult")
        ).build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.requireAccess());

    verify(queryExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
    verify(mutationExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldRun_whenRequireAccess_ifValidQueryAndMutationAccess() throws Exception {
    doReturn(
      Query.builder().withEcho(
        RANDOM_MESSAGE
      ).build())
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());
    doReturn(
      Mutation.builder().withEchoCreate(
        EchoCreatePayload.builder().withEchoes(
          List.of(RANDOM_MESSAGE)
        ).build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    gitlabService.requireAccess();

    verify(queryExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
    verify(mutationExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenCreateBranch_ifGitlabNotReachable() throws Exception {
    doThrow(WebClientRequestException.class)
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.createBranch(PROJECT_PATH, BASE_BRANCH, SOURCE_BRANCH));

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenCreateBranch_ifParameterNull() {
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createBranch(null, BASE_BRANCH, SOURCE_BRANCH));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createBranch(PROJECT_PATH, null, SOURCE_BRANCH));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createBranch(PROJECT_PATH, BASE_BRANCH, null));
  }

  @Test
  void shouldThrowException_whenCreateBranch_ifParameterEmpty() {
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createBranch("", BASE_BRANCH, SOURCE_BRANCH));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createBranch(PROJECT_PATH, "", SOURCE_BRANCH));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createBranch(PROJECT_PATH, BASE_BRANCH, ""));
  }

  @Test
  void shouldRun_whenCreateBranch_ifErrorsOnMutation() throws Exception {
    List<String> errors = List.of("hello", "error");
    doReturn(
      Mutation.builder().withCreateBranch(
        CreateBranchPayload.builder().withErrors(
          errors
        ).build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    GitlabResult gitlabResult = gitlabService.createBranch(PROJECT_PATH, BASE_BRANCH, SOURCE_BRANCH);

    assertThat(gitlabResult).isNotNull();
    assertThat(gitlabResult.getWebUrl()).isEmpty();
    assertThat(gitlabResult.getErrors()).isNotNull().isEqualTo(errors);
    assertThat(gitlabResult.successful()).isFalse();

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldRun_whenCreateBranch_ifNoErrorsOnMutation() throws Exception {
    doReturn(
      Mutation.builder().withCreateBranch(
        CreateBranchPayload.builder().build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    GitlabResult gitlabResult = gitlabService.createBranch(PROJECT_PATH, BASE_BRANCH, SOURCE_BRANCH);

    assertThat(gitlabResult).isNotNull();
    assertThat(gitlabResult.getWebUrl()).isEmpty();
    assertThat(gitlabResult.getErrors()).isNotNull().isEmpty();
    assertThat(gitlabResult.successful()).isTrue();

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenCommit_ifGitlabNotReachable() throws Exception {
    doThrow(WebClientRequestException.class)
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenCommit_ifParameterNull() {
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(null, SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, null, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, null, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, null, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, null, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, null));
  }

  @Test
  void shouldThrowException_whenCommit_ifParameterEmpty() {
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit("", SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, "", FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, "", FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, "", true, CREATE_MESSAGE, UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, "", UPDATE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.commit(PROJECT_PATH, SOURCE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, ""));
  }

  @Test
  void shouldRun_whenCommit_ifErrorsOnMutation() throws Exception {
    List<String> errors = List.of("hello", "error");
    doReturn(
      Mutation.builder().withCommitCreate(
        CommitCreatePayload.builder().withErrors(
          errors
        ).build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    GitlabResult gitlabResult = gitlabService.commit(PROJECT_PATH, BASE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE);

    assertThat(gitlabResult).isNotNull();
    assertThat(gitlabResult.getWebUrl()).isEmpty();
    assertThat(gitlabResult.getErrors()).isNotNull().isEqualTo(errors);
    assertThat(gitlabResult.successful()).isFalse();

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldRun_whenCommit_ifNoErrorsOnMutation() throws Exception {
    doReturn(
      Mutation.builder().withCommitCreate(
        CommitCreatePayload.builder().build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    GitlabResult gitlabResult = gitlabService.commit(PROJECT_PATH, BASE_BRANCH, FILE_NAME, FILE_CONTENT, true, CREATE_MESSAGE, UPDATE_MESSAGE);

    assertThat(gitlabResult).isNotNull();
    assertThat(gitlabResult.getWebUrl()).isEmpty();
    assertThat(gitlabResult.getErrors()).isNotNull().isEmpty();
    assertThat(gitlabResult.successful()).isTrue();

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenCreateMergeRequest_ifGitlabNotReachable() throws Exception {
    doThrow(WebClientRequestException.class)
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());

    assertThrows(IllegalStateException.class,
      () -> gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, BASE_BRANCH, TITLE_MESSAGE));

    verify(mutationExecutor, only()).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldThrowException_whenCreateMergeRequest_ifParameterNull() {
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(null, SOURCE_BRANCH, BASE_BRANCH, TITLE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(PROJECT_PATH, null, BASE_BRANCH, TITLE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, null, TITLE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, BASE_BRANCH, null));
  }

  @Test
  void shouldThrowException_whenCreateMergeRequest_ifParameterEmpty() {
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest("", SOURCE_BRANCH, BASE_BRANCH, TITLE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(PROJECT_PATH, "", BASE_BRANCH, TITLE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, "", TITLE_MESSAGE));
    assertThrows(IllegalArgumentException.class, () -> gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, BASE_BRANCH, ""));
  }

  @Test
  void shouldThrowException_whenCreateMergeRequest_ifNoErrorsOnMutation_ifNoWebUrl() throws Exception {
    mockMergeRequestCreateMutation(List.of());
    mockMergeRequestsQuery(null);

    assertThrows(IllegalStateException.class,
      () -> gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, BASE_BRANCH, TITLE_MESSAGE));

    verify(queryExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
    verify(mutationExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldRun_whenCreateMergeRequest_ifErrorsOnMutation() throws Exception {
    String webUrl = "webUrl";
    List<String> errors = List.of("hello", "error");
    mockMergeRequestCreateMutation(errors);
    mockMergeRequestsQuery(webUrl);

    GitlabResult gitlabResult = gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, BASE_BRANCH, TITLE_MESSAGE);

    assertThat(gitlabResult).isNotNull();
    assertThat(gitlabResult.getWebUrl()).isEqualTo(webUrl);
    assertThat(gitlabResult.getErrors()).isNotNull().isEqualTo(errors);
    assertThat(gitlabResult.successful()).isFalse();

    verify(queryExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
    verify(mutationExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  @Test
  void shouldRun_whenCreateMergeRequest_ifNoErrorsOnMutation_ifWebUrl() throws Exception {
    String webUrl = "webUrl";
    List<String> errors = List.of();
    mockMergeRequestCreateMutation(errors);
    mockMergeRequestsQuery(webUrl);

    GitlabResult gitlabResult = gitlabService.createMergeRequest(PROJECT_PATH, SOURCE_BRANCH, BASE_BRANCH, TITLE_MESSAGE);

    assertThat(gitlabResult).isNotNull();
    assertThat(gitlabResult.getWebUrl()).isEqualTo(webUrl);
    assertThat(gitlabResult.getErrors()).isNotNull().isEqualTo(errors);
    assertThat(gitlabResult.successful()).isTrue();

    verify(queryExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
    verify(mutationExecutor, times(1)).execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  private void mockMergeRequestCreateMutation(List<String> errors) throws GraphQLRequestExecutionException {
    doReturn(
      Mutation.builder().withMergeRequestCreate(
        MergeRequestCreatePayload.builder().withErrors(
          errors
        ).build()
      ).build())
      .when(mutationExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());
  }

  private void mockMergeRequestsQuery(String webUrl) throws GraphQLRequestExecutionException {
    doReturn(
      Query.builder().withProject(
        Project.builder().withMergeRequests(
          MergeRequestConnection.builder().withNodes(
            List.of(MergeRequest.builder().withWebUrl(
              webUrl
            ).build())
          ).build()
        ).build()
      ).build())
      .when(queryExecutor)
      .execWithBindValues(any(ObjectResponse.class), anyMap());
  }
}