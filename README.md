# java-dataloader-guice

## Overview

This library aims to reduce the friction of using java-dataloader in Guice-based projects, especially when combined with graphql-java. 

### Without java-dataloader-guice

Without java-dataloader-guice, you need to manually bind each `DataLoader` and use all of them to construct a `DataLoaderRegistry`. For example, each `DataLoader` would normally need a separate Guice binding such as:

```java
@Provides
@RequestScoped
@Named("greeting")
public DataLoader<String, String> provideGreetingDataLoader(GreetingBatchLoader greetingBatchLoader) {
  return DataLoader.newDataLoader(greetingBatchLoader);
}
```

When dealing with lots of such bindings, it is easy to accidentally inject the wrong `BatchLoader`. 

Next, you would then need to inject all of these `DataLoader`s to build the `DataLoaderRegistry`, for example:

```java
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
```

Keeping all the generic signatures and names in order is tedious and error-prone. And when adding a new `DataLoader`, it is easy to forget to add it to the registry. 

In addition, when used with graphql-java, resolvers are normally singletons which creates an impedance mismatch with request-scoped `DataLoader` instances. This means carefully injecting `Provider`s where needed to avoid `OutOfScopeException`s, for example:

```java
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
```

### With java-dataloader-guice

With java-dataloader-guice, it only takes one line of code to bind each `DataLoader`, for example:
```java
DataLoaderBinder.newDataLoaderBinder(binder)
    .bindDataLoader("greeting").toBatchLoader(GreetingBatchLoader.class)
    .bindDataLoader("farewell").toBatchLoader(FarewellBatchLoader.class)
    .bindDataLoader("congratulations").toBatchLoader(CongratulationsBatchLoader.class);
```

This will create the appropriate `DataLoader` bindings and also add them to a `MapBinder`. This `MapBinder` is used by `DataLoaderModule` to automatically bind a `DataLoaderRegistry` that will always include all your `DataLoader`s. In addition, the `DataLoader`s are bound as singletons and handle the request-scoping internally, which means that they don't need to be wrapped in a `Provider` when injected into graphql-java resolvers.

## Usage

#### Maven dependency

```xml
<dependency>
  <groupId>com.hubspot.dataloader</groupId>
  <artifactId>java-dataloader-guice</artifactId>
  <version>0.1</version>
</dependency
```

#### Binding

Install the `DataLoaderModule`, this handles binding a request-scoped `DataLoaderRegistry`:
```java
binder.install(new DataLoaderModule());
```

Bind all your `BatchLoader`/`MappedBatchLoader`:
```java
DataLoaderBinder.newDataLoaderBinder(binder)
    .bindDataLoader("greeting").toBatchLoader(GreetingBatchLoader.class)
    .bindDataLoader("farewell").toMappedBatchLoader(FarewellBatchLoader.class);
```
