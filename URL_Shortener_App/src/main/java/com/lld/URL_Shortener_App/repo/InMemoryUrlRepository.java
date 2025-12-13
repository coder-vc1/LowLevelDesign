package com.lld.URL_Shortener_App.repo;

import com.lld.URL_Shortener_App.entity.UrlMapping;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
class InMemoryUrlRepository implements UrlRepository {
  // Thread-safe map for storage
  private final Map<String, UrlMapping> storage = new ConcurrentHashMap<>();

  // Thread-safe counter for IDs
  private final AtomicLong sequence = new AtomicLong(10000); // Start at 10k to have non-trivial IDs

  @Override
  public UrlMapping save(UrlMapping mapping) {
    // In real DB, ID is generated on save. Here we set it manually if null.
    if(mapping.getId() == null) {
      mapping.setId(getNextId());
    }
    storage.put(mapping.getShortCode(), mapping);
    return mapping;
  }

  @Override
  public Optional<UrlMapping> findByShortCode(String shortCode) {
    return Optional.ofNullable(storage.get(shortCode));
  }

  @Override
  public Long getNextId() {
    return sequence.incrementAndGet();
  }
}
