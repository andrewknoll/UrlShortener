package urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import urlshortener.repository.ClickRepository;
import urlshortener.repository.QRRepository;
import urlshortener.repository.ShortURLRepository;
import urlshortener.repository.impl.ClickRepositoryImpl;
import urlshortener.repository.impl.QRRepositoryImpl;
import urlshortener.repository.impl.ShortURLRepositoryImpl;

@Configuration
@Import(WebSocketConfig.class)
public class PersistenceConfiguration {

  private final JdbcTemplate jdbc;

  public PersistenceConfiguration(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Bean
  ShortURLRepository shortURLRepository() {
    return new ShortURLRepositoryImpl(jdbc);
  }

  @Bean
  ClickRepository clickRepository() {
    return new ClickRepositoryImpl(jdbc);
  }

  @Bean
  QRRepository QRRepository() {
    return new QRRepositoryImpl(jdbc);
  }

  @Bean(name="asyncWorker")
  public TaskExecutor workExecutor() {
      ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
      threadPoolTaskExecutor.setThreadNamePrefix("Async-");
      threadPoolTaskExecutor.setCorePoolSize(3);
      threadPoolTaskExecutor.setMaxPoolSize(3);
      threadPoolTaskExecutor.setQueueCapacity(600);
      threadPoolTaskExecutor.afterPropertiesSet();
      return threadPoolTaskExecutor;
  }

}
