package ranked;

import arc.struct.Seq;

public abstract class RankedEventType{

    private RankedEventType(){}

    public static class RankedGameStart{
        public final Seq<PlayerData> players;

        public RankedGameStart(Seq<PlayerData> players){
            this.players = players;
        }
    }

    public static class RankedGameEnd{
        public final MatchInfo match;

        public RankedGameEnd(MatchInfo match){
            this.match = match;
        }
    }
}
