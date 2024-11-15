package com.tn.file.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tn.file.domain.FileDeleteException;
import com.tn.file.domain.FileMetadata;
import com.tn.file.domain.FileReadException;
import com.tn.file.domain.FileSaveException;

@RequiredArgsConstructor
public class FileRepositoryImpl implements FileRepository
{
  private static final int INDEX_KEY = 1;
  private static final int INDEX_CONTENT_TYPE = 2;
  private static final int INDEX_NAME = 3;
  private static final int INDEX_SIZE = 4;
  private static final int INDEX_DATA = 5;

  private static final String SQL_SELECT = """
    SELECT file_key, content_type, name, size, data
    FROM file
    WHERE file_key = ?
  """;

  private static final String SQL_UPSERT = """
    MERGE INTO file(file_key, content_type, name, size, data) 
    KEY (file_key) 
    VALUES (?, ?, ?, ?, ?)
  """;

  private static final String SQL_DELETE = """
    DELETE
    FROM file
    WHERE file_key = ?
  """;

  private final DataSource dataSource;

  @Override
  public Optional<FileStream> findForKey(String key)
  {
    return ResultSetFileStream.open(dataSource, key);
  }

  @Override
  public void save(FileMetadata fileMetadata, InputStream fileContent)
  {
    try
    (
      Connection connection = dataSource.getConnection();
      PreparedStatement preparedStatement = upsert(fileMetadata, fileContent, connection)
    )
    {
      if (preparedStatement.executeUpdate() != 1) throw new FileSaveException("File save error: " + fileMetadata.key());
    }
    catch (SQLException e)
    {
      throw new FileSaveException("File save error: " + fileMetadata.key(), e);
    }
  }

  @Override
  public void delete(String key)
  {
    try
    (
      Connection connection = dataSource.getConnection();
      PreparedStatement preparedStatement = delete(key, connection)
    )
    {
      preparedStatement.execute();
    }
    catch (SQLException e)
    {
      throw new FileDeleteException("File delete error: " + key, e);
    }
  }

  private PreparedStatement upsert(FileMetadata fileMetadata, InputStream fileContent, Connection connection) throws SQLException
  {
    PreparedStatement preparedStatement = connection.prepareStatement(SQL_UPSERT);
    preparedStatement.setString(INDEX_KEY, fileMetadata.key());
    preparedStatement.setString(INDEX_CONTENT_TYPE, fileMetadata.contentType());
    preparedStatement.setString(INDEX_NAME, fileMetadata.name());
    preparedStatement.setLong(INDEX_SIZE, fileMetadata.size());
    preparedStatement.setBlob(INDEX_DATA, fileContent);

    return preparedStatement;
  }

  private PreparedStatement delete(String key, Connection connection) throws SQLException
  {
    PreparedStatement preparedStatement = connection.prepareStatement(SQL_DELETE);
    preparedStatement.setString(INDEX_KEY, key);

    return preparedStatement;
  }

  @Slf4j
  private static class ResultSetFileStream implements FileStream
  {
    private final Connection connection;
    private final PreparedStatement preparedStatement;
    private final ResultSet resultSet;
    private final FileMetadata metadata;
    private final InputStream stream;

    private ResultSetFileStream(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) throws SQLException
    {
      this.connection = connection;
      this.preparedStatement = preparedStatement;
      this.resultSet = resultSet;

      this.metadata = new FileMetadata(resultSet.getString(INDEX_KEY), resultSet.getString(INDEX_CONTENT_TYPE), resultSet.getString(INDEX_NAME), resultSet.getLong(INDEX_SIZE));
      this.stream = resultSet.getBlob(INDEX_DATA).getBinaryStream();
    }

    public static Optional<FileStream> open(DataSource dataSource, String key)
    {
      try
      {
        Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = select(key, connection);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (!resultSet.next())
        {
          closeAll(resultSet, preparedStatement, connection);
          return Optional.empty();
        }

        return Optional.of(new ResultSetFileStream(connection, preparedStatement, resultSet));
      }
      catch (SQLException e)
      {
        throw new FileReadException("File read error: " + key, e);
      }
    }

    @Override
    public FileMetadata metadata()
    {
      return metadata;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException
    {
      int i;
      while ((i = stream.read()) >= 0) out.write(i);

      close();
    }

    @Override
    public void close()
    {
      closeAll(resultSet, preparedStatement, connection);
    }

    private static PreparedStatement select(String key, Connection connection) throws SQLException
    {
      PreparedStatement preparedStatement = connection.prepareStatement(SQL_SELECT);
      preparedStatement.setString(INDEX_KEY, key);

      return preparedStatement;
    }

    private static void closeAll(AutoCloseable... autoCloseables)
    {
      for (AutoCloseable autoCloseable : autoCloseables)
      {
        try
        {
          autoCloseable.close();
        }
        catch (Exception e)
        {
          log.warn("Error closing: {}", autoCloseable.getClass(), e);
        }
      }
    }
  }
}
