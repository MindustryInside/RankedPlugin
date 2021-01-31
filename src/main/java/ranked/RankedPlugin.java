package ranked;

import arc.*;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import mindustry.core.NetServer;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.WorldReloader;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static mindustry.Vars.*;

public class RankedPlugin extends Plugin{
    private Configuration configuration;
    private final Interval interval = new Interval(3);
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
    public ObjectMap<String, PlayerData> data = new ObjectMap<>();
    public Seq<MatchInfo> matches = new Seq<>();
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

        rules.tags.put("ranked", "true");

        load();

        Log.debug("matches: @", matches);
        Log.debug("data: @", data);
        Log.debug("discriminatorCounter: @", discriminatorCounter);

        NetServer.TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Seq<Player> arr = Seq.with(players);

            if(active() && !lobby()){
                for(Teams.TeamData data : state.teams.getActive()){
                    if(!arr.contains(p -> p.team() == data.team && p != player) && data.hasCore()){
                        return data.team;
                    }
                }
                return Team.derelict;
            }else{
                return prev.assign(player, players);
            }
        };

        Events.on(EventType.GameOverEvent.class, event -> {
            if(current != null){
                Player winner = Groups.player.find(t -> t.team() == event.winner);
                current.winner = current.players.find(p -> Objects.equals(winner.uuid(), p.uuid));
                if(current.winner == null) return;
                Events.fire(new RankedEventType.RankedGameEnd(current));
            }
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            if(current != null){
                current.winner = current.players.find(p -> p != data.get(event.player.uuid()));
                if(current.winner == null) return;
                // It's real
                Events.fire(new RankedEventType.RankedGameEnd(current));
            }

            ready.remove(event.player.uuid());
        });

        Events.on(RankedEventType.RankedGameEnd.class, event -> {
            current.duration = Time.timeSinceMillis(start);

            PlayerData loser = current.players.get(1);
            PlayerData winner = current.winner;
            winner.rank.rating = newRating(winner, loser, true);
            loser.rank.rating = newRating(loser, winner, false);

            matches.add(current.copy());

            current = null;

            WorldReloader reloader = new WorldReloader();

            reloader.begin();

            loadLobby();
            logic.play();

            reloader.end();
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            Player player = event.player;
            data.get(player.uuid(), () -> new PlayerData(player.uuid(), player.name, discriminatorCounter.getAndIncrement(), configuration.ranks.get(0).copy()));
        });

        Events.run(EventType.Trigger.update, () -> {
            if(interval.get(0, 60 * 60)){
                save();
            }

            if(active()){
                if(lobby() && current == null){
                    if(interval.get(1, 60)){
                        Log.debug("all: @", data);
                        if(configuration.dueling && ready.size / 2 == 1){
                            Events.fire(new RankedEventType.RankedGameStart(Seq.with(ready.map(data::get))));
                        }else{
                            Call.setHudText(Strings.format("In queue: @\nLooking for an opponent@", ready.size, Strings.animated(Time.time, 4, 90f, ".")));
                        }
                    }
                }else{
                    if(current != null && state.isPlaying() && interval.get(2, 60 * 5)){
                        Log.debug("players: @", current.players);
                        PlayerData p1 = current.players.first();
                        if(p1 == null || p1.asPlayer() == null) return;
                        PlayerData p2 = current.players.find(p -> !p.uuid.equals(p1.uuid) && inDiapason(p.rank.rating, p1.rank.rating, 50));
                        if(p2 == null || p2.asPlayer() == null) return; // todo(Skat) idk why it's null
                        // upd: null because this runs on world load

                        Call.infoPopup(p1.asPlayer().con, "[green]\uE804[] " + newRating(p1, p2, true), 5.1f, 200, 1, 1, 1, 1900);
                        Call.infoPopup(p1.asPlayer().con, "[scarlet]\uE805[] " + newRating(p1, p2, false), 5.1f, 200, 60, 1, 1, 1900);

                        Call.infoPopup(p2.asPlayer().con, "[green]\uE804[] " + newRating(p2, p1, true), 5.1f, 200, 1, 1, 1, 1900);
                        Call.infoPopup(p2.asPlayer().con, "[scarlet]\uE805[] " + newRating(p2, p1, false), 5.1f, 200, 60, 1, 1, 1900);
                    }
                }
            }
        });

        Events.on(RankedEventType.RankedGameStart.class, event -> {
            state.rules.tags.remove("lobby");
            WorldReloader reloader = new WorldReloader();

            reloader.begin();

            Map map = maps.all().find(m -> {
                Rules rules = m.rules();
                return rules.pvp || !rules.waves && !rules.attackMode;
            });

            world.loadMap(map);

            state.rules = rules.copy();
            state.rules.pvp = true;
            state.rules.attackMode = true;
            logic.play();

            reloader.end();

            ready.removeAll(event.players.map(p -> p.uuid));

            current = new MatchInfo(event.players, state.map.name());
            start = Time.millis();
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){

        handler.register("ranked", "Begin hosting with the Ranked gamemode", args -> {
            if(!state.isMenu()){
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
            int pos = data.values().toSeq().sort(p -> -p.rank.rating).sort(p -> p.discriminator).indexOf(playerData) + 1;

            Call.infoMessage(player.con, Strings.format("[orange]-- Your Statistic --\n" +
                                                        "name [lightgray]@[]#[lightgray]@[orange]\n" +
                                                        "rank [lightgray]@[]\n" +
                                                        "rating [lightgray]@[]\n" +
                                                        "position in top [lightgray]@",
                                                        playerData.name, playerData.discriminator,
                                                        playerData.rank.name, playerData.rank.rating, pos));
        });
    }

    public void load(){
        matches = gson.fromJson(Core.settings.getString("ranked-matches"), new TypeToken<Seq<MatchInfo>>(){}.getType());
        data = gson.fromJson(Core.settings.getString("ranked-data"), new TypeToken<ObjectMap<String, PlayerData>>(){}.getType());
        discriminatorCounter.set(Core.settings.getInt("discriminator-counter"));

        if(matches == null){
            matches = new Seq<>();
        }

        if(data == null){
            data = new ObjectMap<>();
        }
    }

    public void save(){
        Core.settings.put("ranked-matches", gson.toJson(matches));
        Core.settings.put("ranked-data", gson.toJson(data));
        Core.settings.put("discriminator-counter", discriminatorCounter.get());
    }

    private boolean inDiapason(double f1, double f2, double d){
        return f1 + d > f2 && f2 > f1 - d;
    }

    public double transformed(PlayerData player){
        return Math.pow(10d, player.rank.rating / 400d);
    }

    public double expectedScore(PlayerData p1, PlayerData p2){
        // Ea = 1 / (1 + 10 ^ ((Rb - Ra) / 400))
        double p1Tran = transformed(p1);
        double p2Tran = transformed(p2);
        return p1Tran / (p1Tran + p2Tran);
    }

    public long newRating(PlayerData p1, PlayerData p2){
        return newRating(p1, p2, Objects.equals(current.winner, p1));
    }

    public long newRating(PlayerData p1, PlayerData p2, boolean win){
        // Ra = Ra + K * (Sa — Ea)
        double updatedRating = p1.rank.rating + 15 * (Mathf.num(win) - expectedScore(p1, p2));
        return (long)Math.ceil(updatedRating);
    }

    private void loadLobby(){
        Map map = maps.all().find(m -> m.name().contains("lobby"));
        if(map == null){
            Log.err("Lobby map not found. Please add at least one map with name 'lobby'");
            Core.app.exit();
        }
        world.loadMap(map);
        state.rules = rules.copy();
        state.rules.tags.put("lobby", "true");
    }

    private boolean lobby(){
        return state.rules.tags.getBool("lobby");
    }

    private boolean active(){
        return state.rules.tags.getBool("ranked") && !state.isMenu();
    }
}
