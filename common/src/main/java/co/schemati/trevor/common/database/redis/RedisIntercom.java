package co.schemati.trevor.common.database.redis;

import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.database.DatabaseIntercom;
import co.schemati.trevor.api.database.DatabaseProxy;
import co.schemati.trevor.api.util.Strings;
import co.schemati.trevor.common.util.Protocol;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import pl.memexurer.jedisdatasource.api.JedisDataSource;
import pl.memexurer.jedisdatasource.api.JedisPubSubHandler;

import static co.schemati.trevor.api.database.Database.CHANNEL_DATA;
import static co.schemati.trevor.api.database.Database.CHANNEL_INSTANCE;

public class RedisIntercom implements DatabaseIntercom, JedisPubSubHandler {

  private final Platform platform;
  private final RedisDatabase database;
  private final DatabaseProxy proxy;
  private final Gson gson;

  private final String instance;
  private final JedisDataSource dataSource;

  private final List<String> channels = new ArrayList<>();

  public RedisIntercom(Platform platform, RedisDatabase database, DatabaseProxy proxy, Gson gson,
      JedisDataSource dataSource) {
    this.platform = platform;
    this.database = database;
    this.proxy = proxy;
    this.gson = gson;

    this.instance = platform.getInstanceConfiguration().getID();
    this.dataSource = dataSource;
  }

  @Override
  public void init() {
    dataSource.subscribe(this, Strings.replace(CHANNEL_INSTANCE, instance), CHANNEL_DATA);
  }

  public void add(String... channel) {
    Arrays.stream(channel)
        .map(String::getBytes)
        .forEach(dataSource::subscribe);
    channels.addAll(List.of(channel));
  }

  public void remove(String... channel) {
    Arrays.stream(channel)
        .map(String::getBytes)
        .forEach(dataSource::unsubscribe);
    channels.removeAll(List.of(channel));
  }

  @Override
  public void handle(String channel, byte[] messageRaw) {
    String message = new String(messageRaw);

    database.getExecutor().submit(() -> {
      if (message.trim().length() > 0) {
        if (channel.equals(CHANNEL_DATA)) {
          proxy.onNetworkIntercom(channel, message);
        } else if(channels.contains(channel)) {
          Protocol.deserialize(message, gson).ifPresent(payload ->
              payload.process(platform.getEventProcessor()).post()
          );
        }
      }
    });
  }
}
