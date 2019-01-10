package com.hubspot.dataloader.guice;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.dataloader.BatchLoader;

public class RandomBatchLoader implements BatchLoader<String, Long> {

  @Override
  public CompletionStage<List<Long>> load(List<String> keys) {
    List<Long> values = keys
        .stream()
        .map(key -> ThreadLocalRandom.current().nextLong())
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(values);
  }
}
