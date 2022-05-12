package co.schemati.trevor.api.database.uuid;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UuidTranslatorProxy<T> {

  String getNameFromUuid(UUID player, T resource);

  Map<UUID, String> getNameFromUuids(Collection<UUID> player, T resource);

  UUID getTranslatedUuid(String player, T resource);
}
