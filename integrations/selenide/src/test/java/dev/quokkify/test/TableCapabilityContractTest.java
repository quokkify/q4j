package dev.quokkify.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.quokkify.elements.table.model.TablePageMetadata;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class TableCapabilityContractTest {

  @Test(description = "page metadata validates its range invariants")
  public void validatesPageMetadata() {
    Assertions.assertThat(new TablePageMetadata(2, 4).pageNumber()).isEqualTo(2);
    assertThatThrownBy(() -> new TablePageMetadata(0, 4))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TablePageMetadata(1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TablePageMetadata(5, 4))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
