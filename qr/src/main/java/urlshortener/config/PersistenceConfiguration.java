package urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PersistenceConfiguration {

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
