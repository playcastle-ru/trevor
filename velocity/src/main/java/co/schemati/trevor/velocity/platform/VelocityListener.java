package co.schemati.trevor.velocity.platform;

import co.schemati.trevor.api.database.DatabaseProxy;
import co.schemati.trevor.common.proxy.DatabaseProxyImpl;
import co.schemati.trevor.velocity.TrevorVelocity;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class VelocityListener {

  private final TrevorVelocity plugin;
  private final DatabaseProxy proxy;

  public VelocityListener(TrevorVelocity plugin) {
    this.plugin = plugin;
    this.proxy = plugin.getCommon().getDatabaseProxy();
  }

  @Subscribe
  public void onPlayerConnect(LoginEvent event) {
    Player player = event.getPlayer();
    VelocityUser user = new VelocityUser(player);

    DatabaseProxyImpl.ConnectResult result = proxy.onPlayerConnect(user).join();

    if (!result.isAllowed()) {
      result.getMessage().ifPresent(message ->
              event.setResult(
                ResultedEvent.ComponentResult.denied(serialize(message))
              )
      );
    }
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent event) {
    Player player = event.getPlayer();

    proxy.users().get(player.getUniqueId()).ifPresent(proxy::onPlayerDisconnect);
  }

  @Subscribe
  public void onPlayerServerChange(ServerConnectedEvent event) {
    Player player = event.getPlayer();
    String server = event.getServer().getServerInfo().getName();
    RegisteredServer previous = event.getPreviousServer().orElse(null);
    String previousName = previous != null ? previous.getServerInfo().getName() : "";

    proxy.users().get(player.getUniqueId()).ifPresent(user ->
      proxy.onPlayerServerChange(user, server, previousName)
    );
  }

  @Subscribe(order = PostOrder.LAST)
  public void onServerPing(ProxyPingEvent event) {
    event.setPing(
            event.getPing().asBuilder().onlinePlayers(
                    plugin.getCommon().getInstanceData().getPlayerCount()
            ).build()
    );
  }

  @Subscribe
  public void onMessage(PluginMessageEvent event)  {
    if(!(event.getSource() instanceof ServerConnection))
      return;

    if(event.getIdentifier().getId().equals("multisend")) {
      event.setResult(ForwardResult.handled());

      var input = ByteStreams.newDataInput(event.getData());
      var uuid = new UUID(input.readLong(), input.readLong());
      var serverName = input.readUTF();
      plugin.getProxy().getPlayer(uuid).ifPresentOrElse(player -> {
        var server = plugin.getProxy().getServer(serverName);
        if (server.isEmpty()) {
          player.sendMessage(Component.text("Unknown server!"));
        } else {
          player.createConnectionRequest(server.get()).fireAndForget();
        }
      }, () ->  proxy.changeServer(uuid, serverName));
    }
  }

  private Component serialize(String text) {
    return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
  }
}
