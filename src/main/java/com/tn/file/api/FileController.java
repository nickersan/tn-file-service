package com.tn.file.api;

import java.io.IOException;
import java.io.OutputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tn.file.domain.FileException;
import com.tn.file.domain.FileMetadata;
import com.tn.file.domain.FileReadException;
import com.tn.file.domain.FileSaveException;
import com.tn.file.repository.FileRepository;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class FileController
{
  private final FileRepository fileRepository;

  @GetMapping(path = "/{key}")
  public ResponseEntity<StreamingResponseBody> get(@PathVariable("key") String key)
  {
    return fileRepository.findForKey(key)
      .map(this::streamResponse)
      .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{key}")
  public void update(@PathVariable("key") String key, @RequestPart("file") MultipartFile file)
  {
    try
    {
      fileRepository.save(new FileMetadata(key, file.getContentType(), file.getOriginalFilename(), file.getSize()), file.getInputStream());
    }
    catch (IOException e)
    {
      throw new FileSaveException("File save error: " + key, e);
    }
  }

  @DeleteMapping("/{key}")
  public void delete(@PathVariable("key") String key)
  {
    fileRepository.delete(key);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleFileException(FileException e)
  {
    return ResponseEntity.internalServerError().body(new ErrorResponse(e.getMessage()));
  }

  private ResponseEntity<StreamingResponseBody> streamResponse(FileRepository.FileStream fileStream)
  {
    return ResponseEntity.ok()
      .contentType(MediaType.valueOf(fileStream.metadata().contentType()))
      .body(out -> writeTo(fileStream, out));
  }

  private void writeTo(FileRepository.FileStream fileStream, OutputStream out)
  {
    try
    {
      try (fileStream)
      {
        fileStream.writeTo(out);
      }
    }
    catch (IOException e)
    {
      throw new FileReadException("File read error: " + fileStream.metadata().key(), e);
    }
  }

  public record ErrorResponse(String message) {}
}
