package com.hubspot.dataloader.guice;

import org.dataloader.BatchLoader;
import org.dataloader.MappedBatchLoader;

public interface TypedBindingBuilder {
  DataLoaderBinder toBatchLoader(Class<? extends BatchLoader> loaderClass);
  DataLoaderBinder toMappedBatchLoader(Class<? extends MappedBatchLoader> loaderClass);
}
