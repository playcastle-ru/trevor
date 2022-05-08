package co.schemati.trevor.common.database.redis;

import co.schemati.trevor.api.data.User;
import co.schemati.trevor.api.database.DatabaseConnection;
import co.schemati.trevor.api.database.uuid.UuidTranslator;
import co.schemati.trevor.api.instance.InstanceData;
import co.schemati.trevor.api.network.payload.DisconnectPayload;
import co.schemati.trevor.common.database.redis.uuid.RedisUuidTranslator;
import co.schemati.trevor.common.database.redis.uuid.UuidTranslatorImpl;
import com.google.common.collect.ImmutableList;
import java.util.List;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static co.schemati.trevor.api.util.Strings.replace;
import static co.schemati.trevor.common.database.redis.RedisDatabase.HEARTBEAT;
import static co.schemati.trevor.common.database.redis.RedisDatabase.INSTANCE_PLAYERS;
import static co.schemati.trevor.common.database.redis.RedisDatabase.PLAYER_DATA;
import static co.schemati.trevor.common.database.redis.RedisDatabase.SERVER_PLAYERS;

public class RedisConnection implements DatabaseConnection {

  private final String instance;
  private final Jedis connection;
  private final InstanceData data;

  private final RedisUuidTranslator uuidTranslator;

  public RedisConnection(String instance, Jedis connection, InstanceData data,
     RedisUuidTranslator uuidTranslator) {
    this.instance = instance;
    this.connection = connection;
    this.data = data;
    this.uuidTranslator = uuidTranslator;
  }

  @Override
  public void beat() {
    long timestamp = System.currentTimeMillis();

    connection.hset(HEARTBEAT, instance, String.valueOf(timestamp));

    ImmutableList.Builder<String> builder = ImmutableList.builder();
    int playerCount = 0;

    Map<String, String> heartbeats = connection.hgetAll(HEARTBEAT);
    for (Map.Entry<String, String> entry : heartbeats.entrySet()) {
      long lastBeat = Long.parseLong(entry.getValue());
      if (timestamp <= lastBeat + (30 * 1000)) { // 30 seconds
        builder.add(entry.getKey());

        playerCount += connection.scard(replace(INSTANCE_PLAYERS, entry.getKey()));
      } else {
        connection.hdel(HEARTBEAT, entry.getKey());
        clean(entry.getKey());
        System.out.println("Instance " + entry.getKey() + " was dead, so it got removed.");
      }
    }

    data.update(builder.build(), playerCount);
  }

  @Override
  @Deprecated
  public void update(InstanceData data) {
    // Deprecated, moved to beat.
  }

  @Override
  public boolean create(User user) {
    if (isOnline(user)) {
      return false;
    }

    connection.hmset(replace(PLAYER_DATA, user), user.toDatabaseMap(instance));
    connection.sadd(replace(INSTANCE_PLAYERS, instance), user.toString());

    return true;
  }

  @Override
  public DisconnectPayload destroy(String name, UUID uuid) {
    long timestamp = System.currentTimeMillis();
    uuidTranslator.invalidate(name, uuid, connection);
    destroy(timestamp, uuid);

    return DisconnectPayload.of(instance, uuid, timestamp);
  }

  private void destroy(long timestamp, UUID uuid) {
    setServer(uuid, null);

    connection.srem(replace(INSTANCE_PLAYERS, instance), uuid.toString());
    connection.del(replace(PLAYER_DATA, uuid));
  }

  @Override
  public void setServer(UUID uuid, @Nullable String server) {
    String user = uuid.toString();
    String previous = connection.hget(replace(PLAYER_DATA, user), "server");
    if (previous != null) {
      connection.srem(replace(SERVER_PLAYERS, previous), user);
    }

    if (server != null) {
      connection.sadd(replace(SERVER_PLAYERS, server), user);
      connection.hset(replace(PLAYER_DATA, user), "server", server);
    }
  }

  @Override
  public void setServer(User user, @Nullable String server) {
    setServer(user.uuid(), server);
  }

  @Override
  public boolean isOnline(User user) {
    return connection.hexists(replace(PLAYER_DATA, user), "server");
  }

  @Override
  public boolean isInstanceAlive() {
    long timestamp = System.currentTimeMillis();
    if (connection.hexists(HEARTBEAT, instance)) {
      long lastBeat = Long.parseLong(connection.hget(HEARTBEAT, instance));
      return timestamp >= lastBeat + (20 * 1000); // 20 seconds in terms of milliseconds
    }
    return false;
  }

  @Override
  public Set<String> getServerPlayers(String server) {
    return connection.smembers(replace(SERVER_PLAYERS, server));
  }

  @Override
  public long getServerPlayerCount(String server) {
    return connection.scard(replace(SERVER_PLAYERS, server));
  }

  @Override
  public Set<String> getNetworkPlayers() {
    return data.getInstances().stream()
            .map(name -> connection.smembers(replace(INSTANCE_PLAYERS, name)))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
  }

  @Deprecated
  @Override
  public long getNetworkPlayerCount() {
    return data.getPlayerCount();
  }

  @Override
  public void publish(String channel, String message) {
    connection.publish(channel, message);
  }

  @Deprecated
  @Override
  public void deleteHeartbeat() {
    shutdown();
  }

  @Override
  public void shutdown() {
    connection.hdel(HEARTBEAT, instance);
  }

  @Override
  public String getPlayerServer(UUID uuid) {
    return connection.hget(replace(PLAYER_DATA, uuid), "server");
  }

  @Override
  public void clean(String instance) {
    Pipeline pipeline = connection.pipelined();
    for(String uuid: connection.smembers(replace(INSTANCE_PLAYERS, instance))) {
      destroy(System.currentTimeMillis(), UUID.fromString(uuid));
    }
    pipeline.sync();
  }

  @Override
  public UuidTranslator getUuidTranslator() {
    return new UuidTranslatorImpl(uuidTranslator, connection);
  }

  public void persistUuid(String name, UUID uuid) {
    uuidTranslator.persistInfo(name, uuid, connection);
  }

  @Override
  public void close() {
    connection.close();
  }
}
