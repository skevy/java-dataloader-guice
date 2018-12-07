package com.hubspot.dataloader.guice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.RequestScoper.CloseableScope;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;

public class WithoutDataLoaderBinderExample {

  @Test
  public void test() {
    Injector injector = Guice.createInjector(new Module() {

      @Override
      public void configure(Binder binder) {
        binder.bind(SalutationResolver.class).asEagerSingleton();
        binder.install(new ServletModule());
      }

      @Provides
      @RequestScoped
      @Named("greeting")
      public DataLoader<String, String> provideGreetingDataLoader(GreetingBatchLoader greetingBatchLoader) {
        return DataLoader.newDataLoader(greetingBatchLoader);
      }

      @Provides
      @RequestScoped
      @Named("farewell")
      public DataLoader<String, String> provideFarewellDataLoader(FarewellBatchLoader farewellBatchLoader) {
        return DataLoader.newDataLoader(farewellBatchLoader);
      }

      @Provides
      @RequestScoped
      @Named("congratulations")
      public DataLoader<String, String> provideCongratulationsDataLoader(CongratulationsBatchLoader congratulationsBatchLoader) {
        return DataLoader.newDataLoader(congratulationsBatchLoader);
      }

      @Provides
      @RequestScoped
      public DataLoaderRegistry provideDataLoaderRegistry(
          @Named("greeting") DataLoader<String, String> greetingDataLoader,
          @Named("farewell") DataLoader<String, String> farewellDataLoader,
          @Named("congratulations") DataLoader<String, String> congratulationsDataLoader
      ) {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("greeting", greetingDataLoader);
        registry.register("farewell", farewellDataLoader);
        registry.register("congratulations", congratulationsDataLoader);

        return registry;
      }
    });

    SalutationResolver resolver = injector.getInstance(SalutationResolver.class);

    inRequestScope(() -> {
      CompletableFuture<String> greetingFuture = resolver.sayHello("Bill");
      assertThat(greetingFuture.isDone()).isFalse();

      CompletableFuture<String> farewellFuture = resolver.sayGoodbye("Anne");
      assertThat(farewellFuture.isDone()).isFalse();

      CompletableFuture<String> congratulationsFuture = resolver.congratulate("Travis");
      assertThat(congratulationsFuture.isDone()).isFalse();

      DataLoaderRegistry registry = injector.getInstance(DataLoaderRegistry.class);
      registry.dispatchAll();

      assertThat(greetingFuture.isDone()).isTrue();
      assertThat(greetingFuture.getNow(null)).isEqualTo("Hello Bill");

      assertThat(farewellFuture.isDone()).isTrue();
      assertThat(farewellFuture.getNow(null)).isEqualTo("Goodbye Anne");

      assertThat(congratulationsFuture.isDone()).isTrue();
      assertThat(congratulationsFuture.getNow(null)).isEqualTo("Congratulations Travis");
    });
  }

  private static class SalutationResolver {
    private final Provider<DataLoader<String, String>> greetingDataLoader;
    private final Provider<DataLoader<String, String>> farewellDataLoader;
    private final Provider<DataLoader<String, String>> congratulationsDataLoader;

    @Inject
    public SalutationResolver(
        @Named("greeting") Provider<DataLoader<String, String>> greetingDataLoader,
        @Named("farewell") Provider<DataLoader<String, String>> farewellDataLoader,
        @Named("congratulations") Provider<DataLoader<String, String>> congratulationsDataLoader
    ) {
      this.greetingDataLoader = greetingDataLoader;
      this.farewellDataLoader = farewellDataLoader;
      this.congratulationsDataLoader = congratulationsDataLoader;
    }

    public CompletableFuture<String> sayHello(String name) {
      return greetingDataLoader.get().load(name);
    }

    public CompletableFuture<String> sayGoodbye(String name) {
      return farewellDataLoader.get().load(name);
    }

    public CompletableFuture<String> congratulate(String name) {
      return congratulationsDataLoader.get().load(name);
    }
  }

  private static void inRequestScope(Runnable r) {
    try (CloseableScope scope = ServletScopes.scopeRequest(Collections.emptyMap()).open()) {
      r.run();
    }
  }
}
