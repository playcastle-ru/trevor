package co.schemati.trevor.velocity.command;

import co.schemati.trevor.api.TrevorAPI;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;

public class PlayerListCommand implements SimpleCommand {

  private final TrevorAPI trevorAPI;

  public PlayerListCommand(TrevorAPI trevorAPI) {
    this.trevorAPI = trevorAPI;
  }

  @Override
  public void execute(Invocation invocation) {
    if (invocation.arguments().length == 1 && invocation.arguments()[0].equals("all")) {
      trevorAPI.getDatabase().open().orTimeout(1, TimeUnit.SECONDS).whenComplete((db, ex) -> {
        if(ex != null) {
          ex.printStackTrace();
          invocation.source().sendMessage(Component.text("FAILED"));
        } else {
          invocation.source().sendMessage(
              Component.text("Players: " + String.join(", ",
                  db.getUuidTranslator().getNameFromUuids(db.getNetworkPlayers().stream().map(
                      UUID::fromString).collect(Collectors.toSet())))));
        }
      });
    } else {
      invocation.source().sendMessage(Component.text(
          "There are " + trevorAPI.getInstanceData().getPlayerCount()
              + " players online.\nIf you want to get a full list of player names, use /glist all"));
    }
  }
}
