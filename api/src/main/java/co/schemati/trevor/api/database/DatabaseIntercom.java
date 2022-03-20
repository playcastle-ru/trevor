package co.schemati.trevor.api.database;

/**
 * Represents the intercom used to communicate across the network.
 */
public interface DatabaseIntercom {

  /**
   * Initializes the intercom connection.
   */
  void init();

  /**
   * Subscribes the {@link DatabaseIntercom} to the provided channel.
   *
   * @param channel the channel
   */
  void add(String... channel);

  /**
   * Unsubscribes the {@link DatabaseIntercom} from the provided channel.
   *
   * @param channel the channel
   */
  void remove(String... channel);
}
