package dev.quokkify.reportportal.services;

import java.util.Objects;

import org.mockito.MockitoAnnotations;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParamOverrideTestNgServiceTest {

  private ITestResult mockResult;
  private ITestNGMethod mockMethod;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    mockResult = mock(ITestResult.class);
    mockMethod = mock(ITestNGMethod.class);
    when(mockResult.getMethod()).thenReturn(mockMethod);
    ParamOverrideTestNgService.TEST_CASE_ID.remove();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
    ParamOverrideTestNgService.TEST_CASE_ID.remove();
  }

  @DataProvider(name = "descriptionCases")
  public Object[][] descriptionCases() {
    return new Object[][] {
        {"RP-42", "Verifies login flow",
            "**Test Case ID:** [RP-42]()\n**Description:** Verifies login flow\n"},
        {"RP-42", "",
            "**Test Case ID:** [RP-42]()\n**Description:** NA\n"},
        {"RP-42", null,
            "**Test Case ID:** [RP-42]()\n**Description:** NA\n"},
        {null, "Some description",
            "**Test Case ID:** [null]()\n**Description:** Some description\n"},
    };
  }

  @Test(dataProvider = "descriptionCases",
      description = "getTestCaseDescription builds correct markdown for all combinations")
  public void getTestCaseDescription_returnsExpectedMarkdown(
      String caseId, String description, String expected
  ) {
    if (Objects.nonNull(caseId)) {
      ParamOverrideTestNgService.TEST_CASE_ID.set(caseId);
    }
    when(mockMethod.getDescription()).thenReturn(description);

    String result = ParamOverrideTestNgService.getTestCaseDescription(mockResult);

    assertThat(result).isEqualTo(expected);
  }

  @Test(description = "TEST_CASE_ID ThreadLocal is correctly set and then removed")
  public void testCaseId_threadLocal_isSetAndRemovedCorrectly() {
    assertThat(ParamOverrideTestNgService.TEST_CASE_ID.get()).isNull();

    ParamOverrideTestNgService.TEST_CASE_ID.set("TEST-123");
    assertThat(ParamOverrideTestNgService.TEST_CASE_ID.get()).isEqualTo("TEST-123");

    ParamOverrideTestNgService.TEST_CASE_ID.remove();
    assertThat(ParamOverrideTestNgService.TEST_CASE_ID.get()).isNull();
  }
}
