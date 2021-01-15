package ranked;

import arc.struct.Seq;
import mindustry.maps.Map;

import java.time.Duration;

public final class MatchInfo{
    public Seq<PlayerData> players;
    public Map map;
    public PlayerData winner;
    public Duration duration;
}
