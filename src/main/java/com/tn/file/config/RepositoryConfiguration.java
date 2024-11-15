package com.tn.file.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tn.file.repository.FileRepositoryImpl;

@Configuration
class RepositoryConfiguration
{
  @Bean
  FileRepositoryImpl fileRepositoryImpl(DataSource dataSource)
  {
    return new FileRepositoryImpl(dataSource);
  }
}
