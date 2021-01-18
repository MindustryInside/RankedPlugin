package ranked;

import arc.util.Nullable;

import java.util.Objects;

public final class Rank{
    public String name;
    public long rating;
    public @Nullable Rank next;

    public Rank(String name, long rating, @Nullable Rank next){
        this.name = Objects.requireNonNull(name, "name");
        this.rating = rating;
        this.next = next;
    }

    public Rank(String name, long rating){
        this(name, rating, null);
    }

    public Rank copy(){
        return new Rank(name, rating, next);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Rank rank = (Rank)o;
        return rating == rank.rating &&
               name.equals(rank.name) &&
               Objects.equals(next, rank.next);
    }

    @Override
    public int hashCode(){
        return Objects.hash(name, rating, next);
    }

    @Override
    public String toString(){
        return "Rank{" +
               "name='" + name + '\'' +
               ", rating=" + rating +
               ", next=" + next +
               '}';
    }
}
