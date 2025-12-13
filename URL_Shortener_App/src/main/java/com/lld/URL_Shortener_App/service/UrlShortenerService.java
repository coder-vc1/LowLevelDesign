package com.lld.URL_Shortener_App.service;

import com.lld.URL_Shortener_App.entity.UrlMapping;
import com.lld.URL_Shortener_App.repo.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@RequiredArgsConstructor
@Service
public class UrlShortenerService {

  private final UrlRepository repository;
  private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final char[] CHAR_MAP = ALLOWED_CHARS.toCharArray();
  private static final int BASE = ALLOWED_CHARS.length(); // 62
  private static final String DOMAIN = "http://short.ly/";

  public String shortenUrl(String longUrl) {
    // 1. Get a unique ID (Simulating DB Auto Increment)
    Long id = repository.getNextId();

    // 2. Encode ID to Base62 Short Code
    String shortCode = encodeBase62(id);

    // 3. Save to DB
    UrlMapping mapping = new UrlMapping(id, longUrl, shortCode, LocalDateTime.now());
    repository.save(mapping);

    return shortCode;
  }

  public String getOriginalUrl(String shortCode) {
    return repository.findByShortCode(shortCode)
        .map(UrlMapping::getLongUrl)
        .orElseThrow(() -> new RuntimeException("URL not found for code: " + shortCode));
  }

  // Algorithm: Base 10 (ID) -> Base 62 (String)
  private String encodeBase62(long id) {
    StringBuilder sb = new StringBuilder();
    if (id == 0) return String.valueOf(CHAR_MAP[0]);

    while (id > 0) {
      int remainder = (int) (id % BASE);
      sb.append(CHAR_MAP[remainder]);
      id = id / BASE;
    }
    return sb.reverse().toString();
  }
}