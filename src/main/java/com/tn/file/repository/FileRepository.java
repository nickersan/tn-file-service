package com.tn.file.repository;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import com.tn.file.domain.FileMetadata;

public interface FileRepository
{
  Optional<FileStream> findForKey(String key);

  void save(FileMetadata fileMetadata, InputStream fileContent);

  void delete(String key);

  interface FileStream extends Closeable
  {
    FileMetadata metadata();

    void writeTo(OutputStream out) throws IOException;
  }
}