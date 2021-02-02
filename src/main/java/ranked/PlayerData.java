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
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PlayerData that = (PlayerData)o;
        return discriminator == that.discriminator &&
               uuid.equals(that.uuid) &&
               name.equals(that.name) &&
               rank.equals(that.rank);
    }

    @Override
    public int hashCode(){
        return Objects.hash(uuid, name, discriminator, rank);
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
