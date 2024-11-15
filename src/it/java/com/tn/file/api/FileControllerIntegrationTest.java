package com.tn.file.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;

import com.tn.file.domain.FileMetadata;
import com.tn.file.domain.FileReadException;
import com.tn.file.repository.FileRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileControllerIntegrationTest
{
  private static final String KEY = "test.1";
  private static final String PARAM_FILE = "file";
  private static final Resource RESOURCE_FILE = new ClassPathResource("test.txt");

  @MockBean
  FileRepository fileRepository;

  @Autowired
  TestRestTemplate testRestTemplate;

  @Test
  void shouldFindFile() throws Exception
  {
    var metadata = new FileMetadata(KEY, TEXT_PLAIN_VALUE, RESOURCE_FILE.getFilename(), RESOURCE_FILE.contentLength());

    var fileStream = mock(FileRepository.FileStream.class);
    when(fileStream.metadata()).thenReturn(metadata);
    doAnswer(this::writeFile).when(fileStream).writeTo(any());

    when(fileRepository.findForKey(KEY)).thenReturn(Optional.of(fileStream));

    var response = testRestTemplate.getForEntity("/v1/{key}", String.class, KEY);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(TEXT_PLAIN, response.getHeaders().getContentType());
    assertEquals(RESOURCE_FILE.getContentAsString(StandardCharsets.UTF_8), response.getBody());
  }

  @Test
  void shouldNotFindUnknownFile()
  {
    when(fileRepository.findForKey(KEY)).thenReturn(Optional.empty());

    var response = testRestTemplate.getForEntity("/v1/{key}", String.class, KEY);

    assertTrue(response.getStatusCode().is4xxClientError());
  }

  @Test
  void shouldReturnErrorWhenFindingFile()
  {
    when(fileRepository.findForKey(KEY)).thenThrow(new FileReadException("Testing"));

    var response = testRestTemplate.getForEntity("/v1/{key}", String.class, KEY);

    assertTrue(response.getStatusCode().is5xxServerError());
  }

  @Test
  void shouldReturnErrorWhenReadingFile() throws Exception
  {
    var metadata = new FileMetadata(KEY, TEXT_PLAIN_VALUE, RESOURCE_FILE.getFilename(), RESOURCE_FILE.contentLength());

    var fileStream = mock(FileRepository.FileStream.class);
    when(fileStream.metadata()).thenReturn(metadata);
    doThrow(new IOException()).when(fileStream).writeTo(any());

    when(fileRepository.findForKey(KEY)).thenReturn(Optional.of(fileStream));

    var response = testRestTemplate.getForEntity("/v1/{key}", String.class, KEY);

    assertTrue(response.getStatusCode().is5xxServerError());
  }

  @Test
  void shouldSaveFile() throws Exception
  {
    var multipart = new LinkedMultiValueMap<>();
    multipart.add(PARAM_FILE, RESOURCE_FILE);

    var response = testRestTemplate.exchange("/v1/{key}", PUT, new HttpEntity<>(multipart), Void.class, KEY);

    assertTrue(response.getStatusCode().is2xxSuccessful());

    verify(fileRepository).save(
      eq(new FileMetadata(KEY, TEXT_PLAIN_VALUE, RESOURCE_FILE.getFilename(), RESOURCE_FILE.contentLength())),
      argThat(new InputStreamMatcher())
    );
  }

  @Test
  void shouldDeleteFile()
  {
    var response = testRestTemplate.exchange("/v1/{key}", DELETE, null, Void.class, KEY);

    assertTrue(response.getStatusCode().is2xxSuccessful());

    verify(fileRepository).delete(KEY);
  }

  private Object writeFile(InvocationOnMock invocation) throws Exception
  {
    var out = invocation.getArgument(0, OutputStream.class);
    for (byte b : RESOURCE_FILE.getContentAsByteArray()) out.write(b);

    return null;
  }

  private static class InputStreamMatcher implements ArgumentMatcher<InputStream>
  {
    private final Collection<InputStream> matched = new ArrayList<>();

    @Override
    public boolean matches(InputStream in)
    {
      try
      {
        if (matched.contains(in)) return true;

        boolean result = Arrays.equals(RESOURCE_FILE.getContentAsByteArray(), in.readAllBytes());
        if (result) matched.add(in);

        return result;
      }
      catch (IOException e)
      {
        return false;
      }
    }
  }
}
