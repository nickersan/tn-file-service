package com.tn.file.domain;

public class FileReadException extends FileException
{
  public FileReadException(String message)
  {
    super(message);
  }

  public FileReadException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
