package dev.quokkify.service;

import dev.quokkify.model.ConstantFormat;

/**
 * Type for Uri methods.
 */
public enum UriMethod implements ConstantFormat {
  POST, PUT, PATCH, DELETE;

  @Override
  public String formatValue() {
    return name();
  }
}
