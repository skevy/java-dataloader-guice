package com.hubspot.dataloader.guice;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.dataloader.stats.Statistics;

import com.google.inject.Provider;

public class DataLoaderWrapper<K, V> extends DataLoader<K, V> {
  private final Provider<DataLoader<?, ?>> delegateProvider;

  private DataLoaderWrapper(Provider<DataLoader<?, ?>> delegateProvider) {
    super(key -> null);
    this.delegateProvider = delegateProvider;
  }

  public static <K, V> DataLoader<K, V> wrap(Provider<DataLoader<?, ?>> delegateProvider) {
    return new DataLoaderWrapper<>(delegateProvider);
  }

  @SuppressWarnings("unchecked")
  private DataLoader<K, V> delegate() {
    return (DataLoader<K, V>) delegateProvider.get();
  }

  @Override
  public CompletableFuture<V> load(K key) {
    return delegate().load(key);
  }

  @Override
  public CompletableFuture<V> load(K key, Object keyContext) {
    return delegate().load(key, keyContext);
  }

  @Override
  public CompletableFuture<List<V>> loadMany(List<K> keys) {
    return delegate().loadMany(keys);
  }

  @Override
  public CompletableFuture<List<V>> loadMany(List<K> keys, List<Object> keyContexts) {
    return delegate().loadMany(keys, keyContexts);
  }

  @Override
  public CompletableFuture<List<V>> dispatch() {
    return delegate().dispatch();
  }

  @Override
  public List<V> dispatchAndJoin() {
    return delegate().dispatchAndJoin();
  }

  @Override
  public int dispatchDepth() {
    return delegate().dispatchDepth();
  }

  @Override
  public DataLoader<K, V> clear(K key) {
    return delegate().clear(key);
  }

  @Override
  public DataLoader<K, V> clearAll() {
    return delegate().clearAll();
  }

  @Override
  public DataLoader<K, V> prime(K key, V value) {
    return delegate().prime(key, value);
  }

  @Override
  public DataLoader<K, V> prime(K key, Exception error) {
    return delegate().prime(key, error);
  }

  @Override
  public Object getCacheKey(K key) {
    return delegate().getCacheKey(key);
  }

  @Override
  public Statistics getStatistics() {
    return delegate().getStatistics();
  }
}
