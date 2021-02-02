package ranked;

import java.util.*;

public final class Configuration{
    public boolean dueling = true;
    public List<Rank> ranks = Arrays.asList(
            new Rank("Unranked#2", 10),
            new Rank("Unranked#1", 20)
    );
}
