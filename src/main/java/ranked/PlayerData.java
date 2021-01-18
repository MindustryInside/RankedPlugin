package ranked;

import arc.util.Nullable;
import mindustry.gen.*;

import java.util.Objects;

public final class PlayerData{
    public String uuid;
    public String name;
    public int discriminator;
    public Rank rank;

    public PlayerData(String uuid, String name, int discriminator, Rank rank){
        this.uuid = uuid;
        this.name = name;
        this.discriminator = discriminator;
        this.rank = rank;
    }

    @Nullable
    public Player asPlayer(){
        return Groups.player.find(p -> Objects.equals(uuid, p.uuid()));
    }

    @Override
    public String toString(){
        return "PlayerData{" +
               "uuid='" + uuid + '\'' +
               ", name='" + name + '\'' +
               ", discriminator=" + discriminator +
               ", rank=" + rank +
               '}';
    }
}
