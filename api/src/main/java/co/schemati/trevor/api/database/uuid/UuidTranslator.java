package co.schemati.trevor.api.database.uuid;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UuidTranslator {
  /**
   * Converts UUID to player's name
   * @return player name
   */
  String getNameFromUuid(UUID player);

  /**
   * Converts UUID to player's name
   * @return player name
   */
  List<String> getNameFromUuids(Collection<UUID> player);

  /**
   * Converts name to UUID
   * @return player unique id
   */
  UUID getTranslatedUuid(String player);
}
