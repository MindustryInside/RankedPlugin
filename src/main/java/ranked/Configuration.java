package ranked;

import java.util.*;

public final class Configuration{
    public boolean dueling = true;
    public List<Rank> ranks = Arrays.asList(
            Rank.builder().name("Unranked").prefix("<Y>").build()
    );
}
