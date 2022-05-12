package co.schemati.trevor.bungee.platform;

import co.schemati.trevor.api.network.event.EventProcessor;
import co.schemati.trevor.api.util.Strings;
import co.schemati.trevor.bungee.TrevorBungee;
import co.schemati.trevor.common.platform.AbstractPlatformBase;
import java.util.UUID;
import java.util.logging.Level;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeePlatform extends AbstractPlatformBase {

  private final TrevorBungee plugin;

  private BungeeEventProcessor eventProcessor;

  public BungeePlatform(TrevorBungee plugin) {
    super(null);
    throw new IllegalArgumentException("This platform is no longer supported, lol.");
  }

  public boolean init() {
    if (!super.init()) {
      return false;
    }

    this.eventProcessor = new BungeeEventProcessor(plugin);

    return true;
  }

  @Override
  public EventProcessor getEventProcessor() {
    return eventProcessor;
  }

  @Override
  public boolean isOnlineMode() {
    return plugin.getProxy().getConfig().isOnlineMode();
  }

  @Override
  public void log(String message, Object... values) {
    plugin.getLogger().info(Strings.format(message, values));
  }

  @Override
  public void error(String message, Throwable t) {
    plugin.getLogger().log(Level.SEVERE, message, t) ;
  }

  @Override
  public UUID getLocalPlayerUuid(String name) {
    ProxiedPlayer player = plugin.getProxy().getPlayer(name);
    if(player == null)
      return null;
    return player.getUniqueId();
  }

  @Override
  public String getLocalPlayerName(UUID uuid) {
    ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
    if(player == null)
      return null;
    return player.getName();
  }
}
