package co.schemati.trevor.api.database.uuid;

import java.util.UUID;

public interface UuidTranslatorProxy<T> {

  String getNameFromUuid(UUID player, T resource);

  UUID getTranslatedUuid(String player, T resource);
}
