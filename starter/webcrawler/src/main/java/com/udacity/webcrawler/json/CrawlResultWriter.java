package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
  private final CrawlResult result;

  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  public void write(Path path) {
    Objects.requireNonNull(path);
    try (Writer writer = Files.newBufferedWriter(path)) {
      write(writer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void write(Writer writer) {
    Objects.requireNonNull(writer);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    try {
      objectMapper.writeValue(writer, result);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}