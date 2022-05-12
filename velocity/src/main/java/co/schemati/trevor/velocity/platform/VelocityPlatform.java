package co.schemati.trevor.velocity.platform;

import co.schemati.trevor.api.network.event.EventProcessor;
import co.schemati.trevor.api.util.Strings;
import co.schemati.trevor.common.platform.AbstractPlatformBase;
import co.schemati.trevor.velocity.TrevorVelocity;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.UUID;
import pl.memexurer.jedisdatasource.api.JedisDataSource;
import pl.memexurer.jedisdatasource.api.JedisDataSourceProvider;

public class VelocityPlatform extends AbstractPlatformBase {

  private final TrevorVelocity plugin;

  private VelocityEventProcessor eventProcessor;

  public VelocityPlatform(TrevorVelocity plugin) {
    super(findDataSource(plugin.getProxy()));
    this.plugin = plugin;
  }

  private static JedisDataSource findDataSource(ProxyServer server) {
    for (var plugin : server.getPluginManager().getPlugins()) {
      var dataSource = plugin.getInstance().filter(object -> object instanceof JedisDataSourceProvider)
          .map(object -> (JedisDataSourceProvider) object);
      if (dataSource.isPresent()) {
        return dataSource.get().getDataSource();
      }
    }
    return null;
  }

  public boolean init() {
    if (!super.init()) {
      return false;
    }

    this.eventProcessor = new VelocityEventProcessor(plugin);

    return true;
  }

  @Override
  public EventProcessor getEventProcessor() {
    return eventProcessor;
  }

  @Override
  public boolean isOnlineMode() {
    return plugin.getProxy().getConfiguration().isOnlineMode();
  }

  @Override
  public void log(String message, Object... values) {
    plugin.getLogger().info(Strings.format(message, values));
  }

  @Override
  public void error(String message, Throwable t) {
    plugin.getLogger().error(message, t);
  }

  @Override
  public UUID getLocalPlayerUuid(String name) {
    return plugin.getProxy().getPlayer(name).map(Player::getUniqueId).orElse(null);
  }

  @Override
  public String getLocalPlayerName(UUID uuid) {
    return plugin.getProxy().getPlayer(uuid).map(Player::getUsername).orElse(null);
  }
}
