package com.tn.file.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class NoopController
{
  @GetMapping("/noop")
  public ResponseEntity<Void> noop()
  {
    log.debug("Received GET /noop");
    return ResponseEntity.ok().build();
  }
}
