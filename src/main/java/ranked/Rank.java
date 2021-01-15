package ranked;

import arc.util.Nullable;

import java.util.Objects;

public final class Rank{
    public String name;
    public String prefix;
    public long rating;
    public @Nullable Rank next;

    public Rank(String name, String prefix, long rating, @Nullable Rank next){
        this.name = name;
        this.prefix = prefix;
        this.rating = rating;
        this.next = next;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        public String name;
        public String prefix;
        public long rating;
        public @Nullable Rank next;

        public Builder name(String name){
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public Builder prefix(String prefix){
            this.prefix = Objects.requireNonNull(prefix, "prefix");
            return this;
        }

        public Builder rating(long rating){
            this.rating = rating;
            return this;
        }

        public Builder next(@Nullable Rank next){
            this.next = next;
            return this;
        }

        public Rank build(){
            return new Rank(name, prefix, rating, next);
        }
    }
}
