package ranked;

import arc.struct.Seq;

import java.util.Objects;

public final class MatchInfo{
    public Seq<PlayerData> players;
    public String mapname;
    public PlayerData winner;
    public long duration;

    public MatchInfo(Seq<PlayerData> players, String mapname){
        this.players = players;
        this.mapname = mapname;
    }

    public MatchInfo(Seq<PlayerData> players, String mapname, PlayerData winner, long duration){
        this.players = players;
        this.mapname = mapname;
        this.winner = winner;
        this.duration = duration;
    }

    public MatchInfo copy(){
        return new MatchInfo(players, mapname, winner, duration);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        MatchInfo matchInfo = (MatchInfo)o;
        return duration == matchInfo.duration &&
               players.equals(matchInfo.players) &&
               mapname.equals(matchInfo.mapname) &&
               Objects.equals(winner, matchInfo.winner);
    }

    @Override
    public int hashCode(){
        return Objects.hash(players, mapname, winner, duration);
    }

    @Override
    public String toString(){
        return "MatchInfo{" +
               "players=" + players +
               ", mapname='" + mapname + '\'' +
               ", winner=" + winner +
               ", duration=" + duration +
               '}';
    }
}
