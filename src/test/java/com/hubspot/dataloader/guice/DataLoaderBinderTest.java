package com.hubspot.dataloader.guice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.servlet.RequestScoper.CloseableScope;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;

public class DataLoaderBinderTest {
  private Injector injector;

  @Before
  public void setup() {
    injector = Guice.createInjector(binder -> {
      binder.install(new ServletModule());
      binder.bind(SalutationResolver.class).asEagerSingleton();

      binder.install(new DataLoaderModule());
      DataLoaderBinder.newDataLoaderBinder(binder)
          .bindDataLoader("greeting").toBatchLoader(GreetingBatchLoader.class)
          .bindDataLoader("farewell").toBatchLoader(FarewellBatchLoader.class)
          .bindDataLoader("congratulations").toBatchLoader(CongratulationsBatchLoader.class)
          .bindDataLoader("random").toBatchLoader(RandomBatchLoader.class);
    });
  }

  @Test
  public void test() {
    SalutationResolver resolver = injector.getInstance(SalutationResolver.class);

    inRequestScope(() -> {
      CompletableFuture<String> greetingFuture = resolver.sayHello("Bill");
      assertThat(greetingFuture.isDone()).isFalse();

      CompletableFuture<String> farewellFuture = resolver.sayGoodbye("Anne");
      assertThat(farewellFuture.isDone()).isFalse();

      CompletableFuture<String> congratulationsFuture = resolver.congratulate("Travis");
      assertThat(congratulationsFuture.isDone()).isFalse();

      dispatch();

      assertThat(greetingFuture.isDone()).isTrue();
      assertThat(greetingFuture.getNow(null)).isEqualTo("Hello Bill");

      assertThat(farewellFuture.isDone()).isTrue();
      assertThat(farewellFuture.getNow(null)).isEqualTo("Goodbye Anne");

      assertThat(congratulationsFuture.isDone()).isTrue();
      assertThat(congratulationsFuture.getNow(null)).isEqualTo("Congratulations Travis");
    });
  }

  @Test
  public void dispatchingOneRequestDoesntDispatchOthers() {
    SalutationResolver resolver = injector.getInstance(SalutationResolver.class);

    AtomicReference<CompletableFuture<String>> greetingFuture = new AtomicReference<>();
    inRequestScope(() -> {
      greetingFuture.set(resolver.sayHello("Bill"));
      assertThat(greetingFuture.get().isDone()).isFalse();
    });

    inRequestScope(() -> {
      CompletableFuture<String> otherGreetingFuture = resolver.sayHello("Anne");
      assertThat(otherGreetingFuture.isDone()).isFalse();

      dispatch();

      assertThat(otherGreetingFuture.isDone()).isTrue();
      assertThat(otherGreetingFuture.getNow(null)).isEqualTo("Hello Anne");

    });

    // shouldn't have dispatched this other future
    assertThat(greetingFuture.get().isDone()).isFalse();
  }

  @Test
  public void itCachesWithinARequest() {
    RandomResolver resolver = injector.getInstance(RandomResolver.class);
    String name = "test";

    inRequestScope(() -> {
      CompletableFuture<Long> randomFuture = resolver.getRandom(name);
      assertThat(randomFuture.isDone()).isFalse();

      dispatch();

      assertThat(randomFuture.isDone()).isTrue();

      long firstValue = randomFuture.getNow(null);

      CompletableFuture<Long> secondFuture = resolver.getRandom(name);
      // this assumes it checks the cache eagerly, might be an invalid assumption in the future
      assertThat(secondFuture.isDone()).isTrue();

      long secondValue = secondFuture.getNow(null);

      assertThat(secondValue).isEqualTo(firstValue);
    });
  }

  @Test
  public void itDoesntCacheAcrossRequests() {
    RandomResolver resolver = injector.getInstance(RandomResolver.class);
    String name = "test";

    AtomicLong firstValue = new AtomicLong();
    inRequestScope(() -> {
      CompletableFuture<Long> randomFuture = resolver.getRandom(name);
      assertThat(randomFuture.isDone()).isFalse();

      dispatch();

      assertThat(randomFuture.isDone()).isTrue();
      firstValue.set(randomFuture.getNow(null));
    });

    inRequestScope(() -> {
      CompletableFuture<Long> secondFuture = resolver.getRandom(name);
      assertThat(secondFuture.isDone()).isFalse();

      dispatch();

      assertThat(secondFuture.isDone()).isTrue();
      long secondValue = secondFuture.getNow(null);
      assertThat(secondValue).isNotEqualTo(firstValue.get());
    });
  }

  private void dispatch() {
    injector.getInstance(DataLoaderRegistry.class).dispatchAll();
  }

  private static class RandomResolver {
    private final DataLoader<String, Long> randomDataLoader;

    @Inject
    public RandomResolver(@Named("random") DataLoader<String, Long> randomDataLoader) {
      this.randomDataLoader = randomDataLoader;
    }

    public CompletableFuture<Long> getRandom(String name) {
      return randomDataLoader.load(name);
    }
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
