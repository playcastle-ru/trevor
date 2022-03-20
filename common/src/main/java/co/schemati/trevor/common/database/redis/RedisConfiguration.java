package co.schemati.trevor.common.database.redis;

import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.database.DatabaseConfiguration;
import co.schemati.trevor.api.instance.InstanceData;
import com.google.gson.Gson;
import pl.memexurer.jedisdatasource.api.JedisDataSource;

public class RedisConfiguration implements DatabaseConfiguration {

  private final JedisDataSource dataSource;

  public RedisConfiguration(JedisDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public RedisDatabase create(Platform platform, InstanceData data, Gson gson) {
    return new RedisDatabase(platform, data, dataSource, gson);
  }
}
