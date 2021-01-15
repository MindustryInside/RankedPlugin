package ranked.struct;

import arc.func.*;
import arc.struct.*;

/**
 * ObjectMap with additional convenient methods
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author Skat
 * @see arc.struct.ObjectMap
 */
public class ForwardObjectMap<K, V> extends ObjectMap<K, V>{

    public Seq<V> select(Boolf<V> pred){
        Seq<V> arr = new Seq<>();
        for(Entry<K, V> e : this){
            if(pred.get(e.value)){
                arr.add(e.value);
            }
        }

        return arr;
    }

    public V find(Boolf<V> pred){
        for(Entry<K, V> e : this){
            if(pred.get(e.value)){
                return e.value;
            }
        }

        return null;
    }
}
