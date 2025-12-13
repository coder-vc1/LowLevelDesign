package com.lld.URL_Shortener_App.dto;

import lombok.Data;

@Data
class ShortenRequest {
  private String longUrl;
}