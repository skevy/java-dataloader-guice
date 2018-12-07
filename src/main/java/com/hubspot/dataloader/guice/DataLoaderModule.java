package com.hubspot.dataloader.guice;

import java.util.Map;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;

public class DataLoaderModule implements Module {

  @Override
  public void configure(Binder binder) {
    // make sure the MapBinder is initialized so we can inject the data loader map
    DataLoaderBinder.newDataLoaderBinder(binder);
  }

  @Provides
  @RequestScoped
  public DataLoaderRegistry providesDataLoaderRegistry(Map<String, DataLoader> dataLoaderMap) {
    DataLoaderRegistry registry = new DataLoaderRegistry();
    dataLoaderMap.forEach(registry::register);

    return registry;
  }

  @Override
  public boolean equals(Object o) {
    return o != null && getClass().equals(o.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
