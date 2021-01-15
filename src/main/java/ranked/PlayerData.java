package ranked;

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
}
