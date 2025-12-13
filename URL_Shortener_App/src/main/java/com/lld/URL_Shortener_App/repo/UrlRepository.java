package com.lld.URL_Shortener_App.repo;

import com.lld.URL_Shortener_App.entity.UrlMapping;
import java.util.Optional;

// Interface allows swapping DB later
public interface UrlRepository {
  UrlMapping save(UrlMapping mapping);
  Optional<UrlMapping> findByShortCode(String shortCode);
  Long getNextId(); // Simulates DB Sequence
}