package com.lld.URL_Shortener_App.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlMapping {
  private Long id;
  private String longUrl;
  private String shortCode;
  private LocalDateTime createdDate;
}



