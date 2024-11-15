package com.tn.file.domain;

public class FileSaveException extends FileException
{
  public FileSaveException(String message)
  {
    super(message);
  }

  public FileSaveException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
