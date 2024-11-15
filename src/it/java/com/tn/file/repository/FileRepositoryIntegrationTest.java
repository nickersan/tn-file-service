package com.tn.file.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import com.tn.file.domain.FileMetadata;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileRepositoryIntegrationTest
{
  private static final String KEY = "test.123";
  private static final String NAME = "test.txt";
  private static final String CONTENT_TYPE = "text/plain";
  private static final byte[] CONTENT = "The quick brown fox jumps over the lazy dog".getBytes();

  @Autowired
  private FileRepository fileRepository;

  @Test
  @Order(1)
  @Rollback(false)
  void shouldSave()
  {
    fileRepository.save(new FileMetadata(KEY, CONTENT_TYPE, NAME, CONTENT.length), new ByteArrayInputStream(CONTENT));
  }

  @Test
  @Order(2)
  void shouldFindForKey() throws Exception
  {
    try (var fileStream = fileRepository.findForKey(KEY).orElseThrow(AssertionError::new))
    {
      var out = new ByteArrayOutputStream(CONTENT.length);
      fileStream.writeTo(out);

      assertEquals(new FileMetadata(KEY, CONTENT_TYPE, NAME, CONTENT.length), fileStream.metadata());
      assertArrayEquals(CONTENT, out.toByteArray());
    }
  }

  @Test
  @Order(3)
  void shouldDelete()
  {
    fileRepository.delete(KEY);
    assertTrue(fileRepository.findForKey(KEY).isEmpty());
  }
}
