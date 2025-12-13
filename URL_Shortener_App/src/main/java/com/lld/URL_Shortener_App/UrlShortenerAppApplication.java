package com.lld.URL_Shortener_App;

import com.lld.URL_Shortener_App.service.UrlShortenerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UrlShortenerAppApplication {

  public static void main(String[] args) {
    // 1. Initialize Spring Context
    ConfigurableApplicationContext context = SpringApplication.run(UrlShortenerAppApplication.class, args);

    // 2. Manually fetch the Service Bean
    UrlShortenerService service = context.getBean(UrlShortenerService.class);

    System.out.println("---- STARTING SIMULATION ----");

    // 3. Simulate User Input
    String originalUrl = "https://www.google.com/search?q=system+design+interview";
    System.out.println("Original URL: " + originalUrl);

    // 4. Shorten Logic
    String shortCode = service.shortenUrl(originalUrl);
    System.out.println("Generated Short Code: " + shortCode);
    System.out.println("Full Short URL: http://short.ly/" + shortCode);

    // 5. Redirect/Retrieval Logic
    String retrievedUrl = service.getOriginalUrl(shortCode);
    System.out.println("Retrieved URL from DB: " + retrievedUrl);

    if (originalUrl.equals(retrievedUrl)) {
      System.out.println("SUCCESS: URL mapping works correctly.");
    } else {
      System.out.println("FAILURE: URLs do not match.");
    }

    System.out.println("---- END SIMULATION ----");
  }

}
