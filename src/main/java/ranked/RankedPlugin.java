package ranked;

import arc.Events;
import arc.struct.ObjectSet;
import arc.util.*;
import mindustry.core.GameState;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.mod.Plugin;

import static mindustry.Vars.*;

public class RankedPlugin extends Plugin{
    private Interval interval = new Interval(2);
    private int waitTime = 60 * 60;
    private double counter = 0d;

    private final Rules rules = new Rules();

    public Map next;
    public ObjectSet<String> ready = new ObjectSet<>();

    @Override
    public void init(){
        rules.tags.put("ranked", "true");

        Events.run(EventType.Trigger.update, () -> {
            if(active() && lobby()){
                if((int)(waitTime - counter) / 60 < 0){
                    counter = 0;
                }

                if(interval.get(0, 60)){
                    if(ready.size == getRequiredPlayers()){
                        Call.setHudText("The required number of players has been recruited.\nStarting the game...");
                    }else{
                        int time = (int)(waitTime - counter) / 60;
                        if(time == 0){
                            Call.setHudText("The required number of players is not collected.");
                            next = maps.getShuffleMode().next(Gamemode.survival, state.map);
                        }else{
                            Call.setHudText(Strings.format("Recruited @, required @ players for start game.\nMap @[white]\nwait @ seconds",
                                                           ready.size, getRequiredPlayers(), getNext().name(), time));
                        }
                    }
                }

                counter += Time.delta;
            }else{
                counter = 0;
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){

        handler.register("ranked", "Begin hosting with the Ranked gamemode", args -> {
            if(!state.is(GameState.State.menu)){
                Log.err("Stop the server first.");
                return;
            }

            logic.reset();
            world.loadMap(maps.all().find(m -> m.name().contains("lobby")));
            Rules lobby = rules.copy();
            lobby.tags.put("lobby", "true");
            state.rules = lobby;
            logic.play();
            netServer.openServer();
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("reg", "Get ready for round.", (args, player) -> {
            if(ready.contains(player.uuid())){
                ready.remove(player.uuid());
                player.sendMessage("Unregistered");
            }else{
                ready.add(player.uuid());
                player.sendMessage("Registered");
            }
        });
    }

    private Map getNext(){
        return next != null ? next : (next = maps.getShuffleMode().next(Gamemode.survival, state.map));
    }

    private int getRequiredPlayers(){
        return (int)Math.ceil(Groups.player.size() * 2);
    }

    private boolean lobby(){
        return state.rules.tags.getBool("lobby");
    }

    private boolean active(){
        return state.rules.tags.getBool("ranked") && !state.is(GameState.State.menu);
    }
}
