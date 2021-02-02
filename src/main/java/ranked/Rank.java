package ranked;

import arc.util.Nullable;

import java.util.*;

public final class Rank{
    public String name;
    public long rating;

    public Rank(String name, long rating){
        this.name = Objects.requireNonNull(name, "name");
        this.rating = rating;
    }

    public Rank copy(){
        return new Rank(name, rating);
    }

    public Rank copy(long rating){
        return new Rank(name, rating);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Rank rank = (Rank)o;
        return rating == rank.rating &&
               name.equals(rank.name);
    }

    @Override
    public int hashCode(){
        return Objects.hash(name, rating);
    }

    @Override
    public String toString(){
        return "Rank{" +
               "name='" + name + '\'' +
               ", rating=" + rating +
               '}';
    }
}
