package co.schemati.trevor.common.proxy;

import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.data.User;
import co.schemati.trevor.api.database.Database;
import co.schemati.trevor.api.database.DatabaseConnection;
import co.schemati.trevor.api.database.DatabaseProxy;
import co.schemati.trevor.api.network.payload.ConnectPayload;
import co.schemati.trevor.api.network.payload.DisconnectPayload;
import co.schemati.trevor.api.network.payload.NetworkPayload;
import co.schemati.trevor.api.network.payload.ServerChangePayload;
import co.schemati.trevor.common.TrevorCommon;
import co.schemati.trevor.common.database.redis.RedisDatabase;
import co.schemati.trevor.api.instance.InstanceUserMap;
import co.schemati.trevor.common.util.Protocol;
import com.google.gson.Gson;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DatabaseProxyImpl implements DatabaseProxy {

  private final Platform platform;
  private final Database database;
  private final Gson gson;

  private final String instance;
  private final InstanceUserMap users;

  public DatabaseProxyImpl(Platform platform, Database database) {
    this.platform = platform;
    this.database = database;
    this.gson = TrevorCommon.gson();

    this.instance = platform.getInstanceConfiguration().getID();
    this.users = new InstanceUserMap();
  }

    public CompletableFuture<ConnectResult> onPlayerConnect(User user) {
    return database.open().thenApply(connection -> {
      try {
        if (!connection.isOnline(user)) {
          users.add(user.uuid(), user);

          ConnectPayload payload = ConnectPayload.of(instance, user.uuid(), user.name(), user.address());

          connection.create(user);
          post(RedisDatabase.CHANNEL_DATA, connection, payload);

          return ConnectResult.allow();
        }
        return ConnectResult.deny("&cYou are already logged in.");
      } catch (CompletionException exception) {
        return ConnectResult.deny("&cAn error occurred, please try again.");
      }
    });
  }

  @Override
  public void onPlayerDisconnect(User user) {
    if (!users.remove(user.uuid())) {
      return; // User connection was rejected. Don't fire disconnect.
    }

    long timestamp = System.currentTimeMillis();
    database.open().thenAccept(connection -> {
      DisconnectPayload payload = DisconnectPayload.of(instance, user.uuid(), timestamp);

      connection.destroy(user.name(), user.uuid());
      post(RedisDatabase.CHANNEL_DATA, connection, payload);
    });
  }

  @Override
  public void onPlayerServerChange(User user, String server, String previousServer) {
   database.open().thenAccept(connection -> {
     ServerChangePayload payload =
             ServerChangePayload.of(instance, user.uuid(), server, previousServer);

     connection.setServer(user, server);
     post(RedisDatabase.CHANNEL_DATA, connection, payload);
   });
  }

  @Override
  public void onNetworkIntercom(String channel, String message) {
    Protocol.deserialize(message, gson).ifPresent(payload ->
            payload.process(platform.getEventProcessor()).post()
    );
  }

  @Override
  public void post(String channel, NetworkPayload<?> payload) {
    database.open().thenAccept(connection -> post(channel, connection, payload));
  }

  @Override
  public void post(String channel, DatabaseConnection connection, NetworkPayload<?> payload) {
    connection.publish(channel, Protocol.serialize(payload, gson));
  }

  @Override
  public InstanceUserMap users() {
    return users;
  }
}
