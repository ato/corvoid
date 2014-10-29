package corvoid;

import java.util.Map;
import java.util.Map.Entry;

class Maps {
	static <K,V> void putAllIfAbsent(Map<K,V> dest, Map<K,V> src) {
		for (Entry<K, V> entry : src.entrySet()) {
			if (!dest.containsKey(entry.getKey())) {
				dest.put(entry.getKey(), entry.getValue());
			}
		}
	}		
}