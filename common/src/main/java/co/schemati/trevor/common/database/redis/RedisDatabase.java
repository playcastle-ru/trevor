package co.schemati.trevor.common.database.redis;

import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.database.Database;
import co.schemati.trevor.api.database.DatabaseConnection;
import co.schemati.trevor.api.database.DatabaseIntercom;
import co.schemati.trevor.api.database.DatabaseProxy;
import co.schemati.trevor.api.instance.InstanceData;
import co.schemati.trevor.common.database.redis.uuid.RedisUuidTranslator;
import com.google.gson.Gson;
import pl.memexurer.jedisdatasource.api.JedisDataSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RedisDatabase implements Database {

  public static final String HEARTBEAT = "heartbeat";
  public static final String INSTANCE_PLAYERS = "instance:{}:players";
  public static final String SERVER_PLAYERS = "server:{}:players";
  public static final String PLAYER_DATA = "player:{}";
  public static final String UUID_NAME_DATA = "uuidname:{}";

  private final Platform platform;
  private final String instance;
  private final InstanceData data;
  private final JedisDataSource dataSource;
  private final Gson gson;
  private final ScheduledExecutorService executor;
  private final RedisUuidTranslator translator;

  private RedisIntercom intercom;
  private Future<?> heartbeat;

  public RedisDatabase(Platform platform, InstanceData data, JedisDataSource dataSource, Gson gson) {
    this.platform = platform;
    this.instance = platform.getInstanceConfiguration().getID();
    this.data = data;
    this.dataSource = dataSource;
    this.gson = gson;
    this.executor = Executors.newScheduledThreadPool(8);
    this.translator = new RedisUuidTranslator(platform);
  }

  @Override
  public boolean init(DatabaseProxy proxy) {
    boolean duplicate = open().thenApply(DatabaseConnection::isInstanceAlive).join();
    if (duplicate) {
      platform.log("Duplicate instance detected with instance id: {0}", instance);
      return false;
    }

    platform.log("Cleaning previous data...");
    open().thenAccept(conn -> conn.clean(instance)).join();

    this.heartbeat = executor.scheduleAtFixedRate(this::beat, 5, 5, TimeUnit.SECONDS);

    this.intercom = new RedisIntercom(platform, this, proxy, gson, dataSource);

    intercom.init();

    return true;
  }

  @Override
  public void beat() {
    open().thenAccept(DatabaseConnection::beat);
  }

  @Override
  public CompletableFuture<RedisConnection> open() {
    return dataSource.open(executor)
        .thenApply(resource -> new RedisConnection(platform.getInstanceConfiguration().getID(), resource, data,
            translator));
  }

  @Override
  public DatabaseIntercom getIntercom() {
    return intercom;
  }

  @Override
  public ExecutorService getExecutor() {
    return executor;
  }

  @Override
  public void kill() {
    if (heartbeat != null) {
      heartbeat.cancel(true);
    }
  }

  public RedisUuidTranslator getTranslator() {
    return translator;
  }
}
