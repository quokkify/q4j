package dev.quokkify.flutter.report;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AllureSinkTest {

  private AllureLifecycle original;
  private RecordingWriter writer;
  private AllureLifecycle lifecycle;

  @BeforeMethod
  public void setUp() {
    original = Allure.getLifecycle();
    writer = new RecordingWriter();
    lifecycle = new AllureLifecycle(writer);
    Allure.setLifecycle(lifecycle);
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    Allure.setLifecycle(original);
  }

  @Test
  public void noRunningTestMeansNoAttachment() {
    new AllureSink().attach("Flutter: widget tree", "text/plain", "tree");

    assertThat(writer.sources).isEmpty();
  }

  @Test
  public void attachesToRunningTestWithExtensionForMimeType() {
    AllureExternalKey key = AllureExternalKey.random(AllureSinkTest.class);
    lifecycle.scheduleTest(key, new TestResult().setUuid(UUID.randomUUID().toString()).setName("t"));
    lifecycle.startTest(key);

    new AllureSink().attach("Flutter: widget tree (json)", "application/json", "{}");
    new AllureSink().attach("Flutter: widget tree", "text/plain", "tree");
    lifecycle.stopTest(key);
    lifecycle.writeTest(key);

    assertThat(writer.sources).hasSize(2);
    assertThat(writer.sources.get(0)).endsWith(".json");
    assertThat(writer.sources.get(1)).endsWith(".txt");
    assertThat(writer.results.get(0).getAttachments()).extracting(Attachment::getName, Attachment::getType)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("Flutter: widget tree (json)", "application/json"),
            org.assertj.core.groups.Tuple.tuple("Flutter: widget tree", "text/plain"));
  }

  private static final class RecordingWriter implements AllureResultsWriter {

    private final List<String> sources = new ArrayList<>();
    private final List<TestResult> results = new ArrayList<>();

    @Override
    public void write(TestResult testResult) {
      results.add(testResult);
    }

    @Override
    public void write(TestResultContainer testResultContainer) {
      // containers are irrelevant for attachment assertions
    }

    @Override
    public void write(String source, InputStream attachment) {
      sources.add(source);
    }
  }
}
