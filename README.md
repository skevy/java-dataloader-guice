# java-dataloader-guice

## Overview

This library aims to reduce the friction of using java-dataloader in Guice-based projects, especially when combined with graphql-java.

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
