package ranked;

import arc.struct.Seq;

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
    public String toString(){
        return "MatchInfo{" +
               "players=" + players +
               ", mapname='" + mapname + '\'' +
               ", winner=" + winner +
               ", duration=" + duration +
               '}';
    }
}
