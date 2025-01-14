package co.schemati.trevor.common.database.redis.uuid;

import co.schemati.trevor.api.data.Platform;
import co.schemati.trevor.api.database.uuid.UuidTranslatorProxy;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import redis.clients.jedis.commands.KeyCommands;
import redis.clients.jedis.commands.StringCommands;
import redis.clients.jedis.params.SetParams;

/**
 * @author RedisBungee
 */
public final class RedisUuidTranslator implements UuidTranslatorProxy<StringCommands> {

  private static final String UUID_CACHE_PREFIX = "uuid-cache.";
  private static final Duration CACHE_VALIDITY = Duration.ofDays(3);

  private final Platform platform;

  private final Cache<String, CachedUUIDEntry> nameToUuidCache = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .initialCapacity(128)
      .expireAfterWrite(CACHE_VALIDITY)
      .build();

  private final Cache<UUID, CachedUUIDEntry> uuidToNameCache = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .initialCapacity(128)
      .expireAfterWrite(CACHE_VALIDITY)
      .build();

  public RedisUuidTranslator(Platform platform) {
    this.platform = platform;
  }

  public UUID getTranslatedUuid(String player, StringCommands resource) {
    // If the player is online, give them their UUID.
    // Remember, local data > remote data.
    var localUuid = platform.getLocalPlayerUuid(player);
    if (localUuid != null) {
      return localUuid;
    }

    // Check if it exists in the map
    CachedUUIDEntry cachedUUIDEntry = nameToUuidCache.getIfPresent(player.toLowerCase());
    if (cachedUUIDEntry != null) {
      nameToUuidCache.invalidate(player);
    }

    // Let's try Redis.
    String stored = resource.get(UUID_CACHE_PREFIX + player.toLowerCase());
    if (stored != null) {
      // Found an entry value. Deserialize it.
      var entry = CachedUUIDEntry.parse(stored);

      nameToUuidCache.put(player.toLowerCase(), entry);
      uuidToNameCache.put(entry.getUuid(), entry);
      return entry.getUuid();
    }
    return null;
  }

  public String getNameFromUuid(UUID player, StringCommands resource) {
    // If the player is online, give them their UUID.
    // Remember, local data > remote data.
    var localName = platform.getLocalPlayerName(player);
    if (localName != null) {
      return localName;
    }

    // Check if it exists in the map
    CachedUUIDEntry cachedUUIDEntry = uuidToNameCache.getIfPresent(player);
    if (cachedUUIDEntry != null) {
      return cachedUUIDEntry.getName();
    }

    // Okay, it wasn't locally cached. Let's try Redis.
    String stored = resource.get(UUID_CACHE_PREFIX + player);
    if (stored != null) {
      // Found an entry value. Deserialize it.
      var entry = CachedUUIDEntry.parse(stored);

      nameToUuidCache.put(entry.getName().toLowerCase(), entry);
      uuidToNameCache.put(player, entry);
      return entry.getName();
    }

    return null;
  }

  @Override
  public Map<UUID, String> getNameFromUuids(Collection<UUID> collectionPlayers, StringCommands resource) {
    List<UUID> players = new ArrayList<>(collectionPlayers);
    Map<UUID, String> playerNames = new HashMap<>();

    var playerIterator = players.iterator();
    while (playerIterator.hasNext()) {
      var player = playerIterator.next();
      // If the player is online, give them their UUID.
      // Remember, local data > remote data.
      var localName = platform.getLocalPlayerName(player);
      if (localName != null) {
        playerNames.put(player, localName);
        playerIterator.remove();
        continue;
      }

      // Check if it exists in the map
      CachedUUIDEntry cachedUUIDEntry = uuidToNameCache.getIfPresent(player);
      if (cachedUUIDEntry != null) {
        playerNames.put(player, cachedUUIDEntry.name);
        playerIterator.remove();
      }
    }

    if(players.isEmpty())
      return playerNames;

    // Okay, it wasn't locally cached. Let's try Redis.
    List<String> storedValues = resource.mget(players.stream()
        .map(str -> UUID_CACHE_PREFIX + str)
        .toArray(String[]::new));
    for(String storedEntry: storedValues) {
        var entry = CachedUUIDEntry.parse(storedEntry);

        playerNames.put(entry.getUuid(), entry.getName().toLowerCase());
        nameToUuidCache.put(entry.getName().toLowerCase(), entry);
        uuidToNameCache.put(entry.uuid, entry);
    }

    return playerNames;
  }

  public void persistInfo(String name, UUID uuid, StringCommands jedis) {
    var entry = new CachedUUIDEntry(name, uuid);

    nameToUuidCache.put(name.toLowerCase(), entry);
    uuidToNameCache.put(uuid, entry);

    jedis.set(UUID_CACHE_PREFIX + uuid, entry.toString(), new SetParams()
        .ex(CACHE_VALIDITY.toSeconds()));
    jedis.set(UUID_CACHE_PREFIX + name.toLowerCase(), entry.toString(), new SetParams()
        .ex(CACHE_VALIDITY.toSeconds()));
  }

  public void invalidate(String name, UUID player, KeyCommands jedis) {
    nameToUuidCache.invalidate(name.toLowerCase());
    uuidToNameCache.invalidate(player);

    jedis.del(UUID_CACHE_PREFIX + player, UUID_CACHE_PREFIX + name.toLowerCase());
  }


  private static class CachedUUIDEntry {

    private final String name;
    private final UUID uuid;

    private CachedUUIDEntry(String name, UUID uuid) {
      this.name = name;
      this.uuid = uuid;
    }

    private static CachedUUIDEntry parse(String string) {
      String[] splitted = string.split(":");
      return new CachedUUIDEntry(splitted[0], UUID.fromString(splitted[1]));
    }

    public String getName() {
      return name;
    }

    public UUID getUuid() {
      return uuid;
    }

    @Override
    public String toString() {
      return name + ":" + uuid;
    }
  }
}
