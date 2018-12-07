package com.hubspot.dataloader.guice;

import java.lang.reflect.Type;

import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.MappedBatchLoader;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletScopes;

public class DataLoaderBinder {
  private static final TypeResolver TYPE_RESOLVER = new TypeResolver();

  private final Binder binder;
  private final MapBinder<String, DataLoader> mapBinder;

  private DataLoaderBinder(Binder binder) {
    this.binder = binder;
    this.mapBinder = MapBinder.newMapBinder(binder, String.class, DataLoader.class);
  }

  public static DataLoaderBinder newDataLoaderBinder(Binder binder) {
    return new DataLoaderBinder(binder);
  }

  public TypedBindingBuilder bindDataLoader(String name) {
    return new TypedBindingBuilder() {

      @Override
      @SuppressWarnings("unchecked")
      public DataLoaderBinder toBatchLoader(Class<? extends BatchLoader> loaderClass) {
        Key<DataLoader<?, ?>> key = createKey(name, loaderClass, BatchLoader.class);
        // bind the data loader directly so it's available for injection
        binder.bind(key).toProvider(new Provider<DataLoader<?, ?>>() {

          @Inject
          Injector injector;

          @Override
          public DataLoader<?, ?> get() {
            // use a provider so that the DataLoader can be a singleton
            Provider<BatchLoader> loaderProvider =
                injector.getProvider((Class<BatchLoader>) loaderClass);
            return DataLoader.newDataLoader(keys -> loaderProvider.get().load(keys));
          }
        }).in(Scopes.SINGLETON);

        // also add to the map binder which we use to build the registry
        mapBinder.addBinding(name).to(key).in(ServletScopes.REQUEST);
        return DataLoaderBinder.this;
      }

      @Override
      @SuppressWarnings("unchecked")
      public DataLoaderBinder toMappedBatchLoader(Class<? extends MappedBatchLoader> loaderClass) {
        Key<DataLoader<?, ?>> key = createKey(name, loaderClass, MappedBatchLoader.class);
        // bind the data loader directly so it's available for injection
        binder.bind(key).toProvider(new Provider<DataLoader<?, ?>>() {

          @Inject
          Injector injector;

          @Override
          public DataLoader<?, ?> get() {
            // use a provider so that the DataLoader can be a singleton
            Provider<MappedBatchLoader> loaderProvider =
                injector.getProvider((Class<MappedBatchLoader>) loaderClass);
            return DataLoader.newMappedDataLoader(keys -> loaderProvider.get().load(keys));
          }
        }).in(Scopes.SINGLETON);

        // also add to the map binder which we use to build the registry
        mapBinder.addBinding(name).to(key).in(ServletScopes.REQUEST);
        return DataLoaderBinder.this;
      }
    };
  }

  /**
   * Extract the type parameters from the batch loader in order to construct the data loader generic type
   * For example, if you have: public class FooBatchLoader implements BatchLoader<Integer, Foo>
   * Then the generic type we want to construct is DataLoader<Integer, Foo>
   */
  @SuppressWarnings("unchecked")
  private static Key<DataLoader<?, ?>> createKey(String name, Class<?> loaderImpl, Class<?> loaderType) {
    ResolvedType resolvedType = TYPE_RESOLVER.resolve(loaderImpl);
    // need to convert these because Guice's MoreTypes#isFullySpecified doesn't like ResolvedType
    Type[] loaderParams = resolvedType
        .typeParametersFor(loaderType)
        .stream()
        .map(DataLoaderBinder::convertResolvedType)
        .toArray(Type[]::new);

    Type dataLoaderType =
        new ParameterizedTypeImpl(null, DataLoader.class, loaderParams);
    return (Key<DataLoader<?, ?>>) Key.get(dataLoaderType, Names.named(name));
  }

  private static Type convertResolvedType(ResolvedType resolvedType) {
    if (resolvedType.getTypeParameters().isEmpty()) {
      // no type params should mean regular class
      return resolvedType.getErasedType();
    } else {
      Type[] typeParameters = resolvedType
          .getTypeParameters()
          .stream()
          .map(DataLoaderBinder::convertResolvedType)
          .toArray(Type[]::new);

      return new ParameterizedTypeImpl(null, resolvedType.getErasedType(), typeParameters);
    }
  }
}

