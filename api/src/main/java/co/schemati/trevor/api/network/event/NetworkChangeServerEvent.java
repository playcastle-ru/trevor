package co.schemati.trevor.api.network.event;

import java.util.UUID;

/**
 * Represents a {@link co.schemati.trevor.api.data.User} changing servers on the network.
 */
public interface NetworkChangeServerEvent extends NetworkEvent {

  /**
   * The {@link co.schemati.trevor.api.data.User}'s {@link UUID}.
   *
   * @return the uuid
   */
  UUID uuid();

  /**
   * The {@link co.schemati.trevor.api.data.User}'s new server.
   *
   * @return the new server
   */
  String server();
}
