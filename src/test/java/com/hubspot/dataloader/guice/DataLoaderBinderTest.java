package com.hubspot.dataloader.guice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.servlet.RequestScoper.CloseableScope;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;

public class DataLoaderBinderTest {

  @Test
  public void test() {
    Injector injector = Guice.createInjector(binder -> {
      binder.install(new ServletModule());
      binder.bind(SalutationResolver.class).asEagerSingleton();

      binder.install(new DataLoaderModule());
      DataLoaderBinder.newDataLoaderBinder(binder)
          .bindDataLoader("greeting").toBatchLoader(GreetingBatchLoader.class)
          .bindDataLoader("farewell").toBatchLoader(FarewellBatchLoader.class)
          .bindDataLoader("congratulations").toBatchLoader(CongratulationsBatchLoader.class);
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
    private final DataLoader<String, String> greetingDataLoader;
    private final DataLoader<String, String> farewellDataLoader;
    private final DataLoader<String, String> congratulationsDataLoader;

    @Inject
    public SalutationResolver(
        @Named("greeting") DataLoader<String, String> greetingDataLoader,
        @Named("farewell") DataLoader<String, String> farewellDataLoader,
        @Named("congratulations") DataLoader<String, String> congratulationsDataLoader
    ) {
      this.greetingDataLoader = greetingDataLoader;
      this.farewellDataLoader = farewellDataLoader;
      this.congratulationsDataLoader = congratulationsDataLoader;
    }

    public CompletableFuture<String> sayHello(String name) {
      return greetingDataLoader.load(name);
    }

    public CompletableFuture<String> sayGoodbye(String name) {
      return farewellDataLoader.load(name);
    }

    public CompletableFuture<String> congratulate(String name) {
      return congratulationsDataLoader.load(name);
    }
  }

  private static void inRequestScope(Runnable r) {
    try (CloseableScope scope = ServletScopes.scopeRequest(Collections.emptyMap()).open()) {
      r.run();
    }
  }
}
