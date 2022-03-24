package co.schemati.trevor.common.database.redis.uuid;

import co.schemati.trevor.api.database.uuid.UuidTranslator;
import co.schemati.trevor.api.database.uuid.UuidTranslatorProxy;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.commands.StringCommands;

public class UuidTranslatorImpl implements UuidTranslator {
  private final UuidTranslatorProxy<StringCommands> proxy;
  private final Jedis jedis;

  public UuidTranslatorImpl(
      UuidTranslatorProxy<StringCommands> proxy, Jedis jedis) {
    this.proxy = proxy;
    this.jedis = jedis;
  }

  @Override
  public String getNameFromUuid(UUID player) {
    return proxy.getNameFromUuid(player, jedis);
  }

  @Override
  public UUID getTranslatedUuid(String player) {
    return proxy.getTranslatedUuid(player, jedis);
  }
}
