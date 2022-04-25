package co.schemati.trevor.common;

import co.schemati.trevor.api.TrevorAPI;
import co.schemati.trevor.api.TrevorService;
import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.database.Database;
import co.schemati.trevor.api.instance.InstanceData;
import co.schemati.trevor.common.database.redis.RedisDatabase;
import co.schemati.trevor.common.platform.AbstractPlatformBase;
import co.schemati.trevor.common.proxy.DatabaseProxyImpl;
import com.google.gson.Gson;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TrevorCommon implements TrevorAPI {

  private static Gson gson;

  private final AbstractPlatformBase platform;

  private RedisDatabase database;
  private DatabaseProxyImpl proxy;

  private InstanceData data;

  public TrevorCommon(AbstractPlatformBase platform) {
    this.platform = platform;
  }

  public boolean load() {
    TrevorService.setAPI(this);

    // TODO: Verify instance configuration values before pool creation
    gson = new Gson();

    this.data = new InstanceData();

    this.database = (RedisDatabase) platform.getDatabaseConfiguration().create(platform, data, gson);

    this.proxy = new DatabaseProxyImpl(platform, database);

    return true;
  }

  public boolean start() {
    return database.init(proxy);
  }

  public boolean stop() {
    if (database != null) {
      try {
        database.open().get(1, TimeUnit.SECONDS).shutdown();
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new IllegalArgumentException("Unable to fetch RedisConnection instance!");
      }

      database.kill();
    }
    return true;
  }

  public InstanceData getInstanceData() {
    return data;
  }

  public Platform getPlatform() {
    return platform;
  }

  public Database getDatabase() {
    return database;
  }

  public DatabaseProxyImpl getDatabaseProxy() {
    return proxy;
  }

  public static Gson gson() {
    return gson;
  }
}
