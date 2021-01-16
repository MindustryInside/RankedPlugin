package ranked;

import arc.*;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.*;
import com.google.gson.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.WorldReloader;
import ranked.struct.ForwardObjectMap;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static mindustry.Vars.*;

public class RankedPlugin extends Plugin{
    private Interval interval = new Interval(3);

    private Configuration configuration;
    private final Rules rules = new Rules();
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    public @Nullable MatchInfo current;
    public long start;
    public Seq<String> ready = new Seq<>();
    public ForwardObjectMap<String, PlayerData> data = new ForwardObjectMap<>();
    public AtomicInteger discriminatorCounter = new AtomicInteger();

    @Override
    public void init(){
        Fi cfg = dataDirectory.child("config-ranked.json");
        if(!cfg.exists()){
            cfg.writeString(gson.toJson(configuration = new Configuration()));
            Log.info("Configuration created...");
        }else{
            configuration = gson.fromJson(cfg.reader(), Configuration.class);
        }

        discriminatorCounter.set(Core.settings.getInt("discriminator-counter"));

        rules.tags.put("ranked", "true");

        NetServer.TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Seq<Player> arr = Seq.with(players);

            if(active() && !lobby()){
                for(Teams.TeamData team : state.teams.active){
                    if(team.active() && !arr.contains(p -> p.team() == team.team)){
                        return team.team;
                    }
                }
                return Team.derelict;
            }else{
                return prev.assign(player, players);
            }
        };

        Events.on(EventType.PlayerLeave.class, event -> {
            if(current != null){
                Events.fire(new RankedEventType.RankedGameEnd(current));
            }

            ready.remove(event.player.uuid());
        });

        Events.on(RankedEventType.RankedGameEnd.class, event -> {
            event.match.duration = Duration.ofMillis(Time.timeSinceMillis(start));
            current = null;

            WorldReloader reloader = new WorldReloader();

            reloader.begin();

            loadLobby();
            logic.play();

            reloader.end();
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            Player player = event.player;
            data.get(player.uuid(), () -> new PlayerData(player.uuid(), player.name, discriminatorCounter.getAndIncrement(), configuration.ranks.get(0)));
        });

        Events.run(EventType.Trigger.update, () -> {
            if(interval.get(0, 60 * 60)){
                Core.settings.put("discriminator-counter", discriminatorCounter.get());
            }

            if(active()){
                if(lobby() && current == null){
                    if(interval.get(1, 60)){
                        if(configuration.dueling && ready.size / 2 == 1){
                            PlayerData player1 = data.get(ready.random());
                            PlayerData player2 = data.get(ready.random(player1.uuid));

                            Events.fire(new RankedEventType.RankedGameStart(Seq.with(player1, player2)));
                        }else{
                            Call.setHudText(Strings.format("In queue: @\nLooking for an opponent@", ready.size, Strings.animated(Time.time, 4, 90f, ".")));
                        }
                    }
                }else{
                    if(current != null && interval.get(2, 60 * 5)){
                        current.players.each(p -> {
                            Player player = Groups.player.find(t -> Objects.equals(p.uuid, t.uuid()));
                            if(player == null) return; // todo(Skat) idk why it's null
                            long up = p.rank.rating + (int)Math.ceil(current.players.size * configuration.baseMultiplier);
                            long down = p.rank.rating - (int)Math.ceil(current.players.size * configuration.baseMultiplier);
                            Call.infoPopup(player.con, "\uE804 " + up, 5.1f, 200, 1, 1, 1, 1900);
                            Call.infoPopup(player.con, "\uE805 " + down, 5.1f, 200, 60, 1, 1, 1900);
                        });
                    }
                }
            }
        });

        Events.on(RankedEventType.RankedGameStart.class, event -> {
            state.rules.tags.remove("lobby");
            WorldReloader reloader = new WorldReloader();

            reloader.begin();

            Map map = maps.getNextMap(Gamemode.pvp, null); //maps.all().find(m -> m.teams.size > 1);
            world.loadMap(map);

            state.rules = rules.copy();
            // state.rules.pvp = false;          for fast testing
            // state.rules.waitEnemies = false;
            // state.rules.attackMode = false;
            logic.play();

            reloader.end();

            ready.removeAll(event.players.map(p -> p.uuid));

            current = new MatchInfo();
            current.players = event.players;
            current.map = state.map;
            start = Time.millis();
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
            loadLobby();
            logic.play();
            netServer.openServer();
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("reg", "Get ready for round.", (args, player) -> {
            if(ready.contains(player.uuid())){
                ready.remove(player.uuid());
                player.sendMessage("[lightgray]You've been [orange]removed[] from queue");
            }else{
                ready.add(player.uuid());
                player.sendMessage("[lightgray]You've been [orange]added[] to queue.");
            }
        });

        handler.<Player>register("info", "Get self info.", (args, player) -> {
            PlayerData playerData = data.get(player.uuid());

            Call.infoMessage(player.con, Strings.format("[orange]-- Your Statistic --\n" +
                                                        "name [lightgray]@[]#[lightgray]@[orange]\n" +
                                                        "rank [lightgray]@[]\n" +
                                                        "rating [lightgray]@[]\n" +
                                                        "position in top [lightgray]@",
                                                        playerData.name, playerData.discriminator,
                                                        playerData.rank.name, playerData.rank.rating, 0)); // todo(Skat) make position calculating
        });
    }

    private void loadLobby(){
        world.loadMap(maps.all().find(m -> m.name().contains("lobby")));
        state.rules = rules.copy();
        state.rules.tags.put("lobby", "true");
    }

    private boolean lobby(){
        return state.rules.tags.getBool("lobby");
    }

    private boolean active(){
        return state.rules.tags.getBool("ranked") && !state.is(GameState.State.menu);
    }
}
