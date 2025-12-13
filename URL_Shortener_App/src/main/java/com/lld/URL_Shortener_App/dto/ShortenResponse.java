package com.lld.URL_Shortener_App.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
class ShortenResponse {
  private String shortUrl;
  private String shortCode;
}