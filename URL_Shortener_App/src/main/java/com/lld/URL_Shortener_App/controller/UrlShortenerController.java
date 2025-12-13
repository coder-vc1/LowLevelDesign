package com.lld.URL_Shortener_App.controller;

import com.lld.URL_Shortener_App.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UrlShortenerController {

  private final UrlShortenerService service;

  // API to Shorten
  @PostMapping("/shorten")
  public ResponseEntity<String> shorten(@RequestBody String longUrl) {
    String shortCode = service.shortenUrl(longUrl);
    return ResponseEntity.ok("http://short.ly/" + shortCode);
  }

  // API to Redirect
  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
    String longUrl = service.getOriginalUrl(shortCode);

    // Return 302 Found (Temporary Redirect)
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(longUrl))
        .build();
  }
}
