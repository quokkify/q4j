package dev.quokkify.testrail.models;

public enum StatusId {
  PASSED(1),
  BLOCKED(2),
  UNTESTED(3),
  RETEST(4),
  FAILED(5);

  private final int id;

  StatusId(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
}
