package co.schemati.trevor.api.network.payload;

import co.schemati.trevor.api.network.event.EventProcessor;
import java.util.UUID;

/**
 * Used when someone needs to be moved to another server
 */
public class ChangeServerPayload extends OwnedPayload{
  private final String destinationServer;

  /**
   * Constructs a new OwnedPayload.
   * @param source the source
   * @param uuid the uuid
   * @param destinationServer server which player needs to be moved to
   */
  protected ChangeServerPayload(String source, UUID uuid, String destinationServer) {
    super(source, uuid);
    this.destinationServer = destinationServer;
  }

  public String getDestinationServer() {
    return destinationServer;
  }

  @Override
  public EventProcessor.EventAction<?> process(EventProcessor processor) {
    return processor.onChangeServer(this);
  }

  public static ChangeServerPayload of(String source, UUID uuid, String destinationServer) {
    return new ChangeServerPayload(source, uuid, destinationServer);
  }
}
