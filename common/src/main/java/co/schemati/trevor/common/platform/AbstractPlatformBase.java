package co.schemati.trevor.common.platform;

import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.database.DatabaseConfiguration;
import co.schemati.trevor.api.instance.InstanceConfiguration;
import co.schemati.trevor.common.database.redis.RedisConfiguration;

import co.schemati.trevor.common.database.redis.RedisDatabase;
import pl.memexurer.jedisdatasource.api.JedisDataSource;

public abstract class AbstractPlatformBase implements Platform {

  private InstanceConfiguration instanceConfiguration;
  private DatabaseConfiguration databaseConfiguration;

  private final JedisDataSource dataSource;

  protected AbstractPlatformBase(JedisDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean init() {
    this.instanceConfiguration = new InstanceConfiguration(System.getenv("proxy-id"));
    if(this.instanceConfiguration.getID() == null) {
      System.out.println("Environment variable `proxy-id` is not set!");
      return false;
    }

    this.databaseConfiguration = new RedisConfiguration(dataSource);

    return true;
  }

  @Override
  public InstanceConfiguration getInstanceConfiguration() {
    return instanceConfiguration;
  }

  @Override
  public DatabaseConfiguration getDatabaseConfiguration() {
    return databaseConfiguration;
  }
}
