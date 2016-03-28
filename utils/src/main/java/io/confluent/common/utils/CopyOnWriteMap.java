package io.confluent.common.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * 简单的读优化Map实现类，只能同步写，并且在每次修改后可以完全拷贝
 *
 * @author wanggang
 *
 * @param <K>   键类型
 * @param <V>   值类型
 */
public class CopyOnWriteMap<K, V> implements ConcurrentMap<K, V> {

	private volatile Map<K, V> map;

	public CopyOnWriteMap() {
		this.map = Collections.emptyMap();
	}

	public CopyOnWriteMap(Map<K, V> map) {
		this.map = Collections.unmodifiableMap(map);
	}

	@Override
	public boolean containsKey(Object k) {
		return map.containsKey(k);
	}

	@Override
	public boolean containsValue(Object v) {
		return map.containsValue(v);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Object k) {
		return map.get(k);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public synchronized void clear() {
		this.map = Collections.emptyMap();
	}

	@Override
	public synchronized V put(K k, V v) {
		Map<K, V> copy = new HashMap<K, V>(this.map);
		V prev = copy.put(k, v);
		this.map = Collections.unmodifiableMap(copy);
		return prev;
	}

	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> entries) {
		Map<K, V> copy = new HashMap<K, V>(this.map);
		copy.putAll(entries);
		this.map = Collections.unmodifiableMap(copy);
	}

	@Override
	public synchronized V remove(Object key) {
		Map<K, V> copy = new HashMap<K, V>(this.map);
		V prev = copy.remove(key);
		this.map = Collections.unmodifiableMap(copy);
		return prev;
	}

	@Override
	public synchronized V putIfAbsent(K k, V v) {
		if (!containsKey(k)) {
			return put(k, v);
		} else {
			return get(k);
		}
	}

	@Override
	public synchronized boolean remove(Object k, Object v) {
		if (containsKey(k) && get(k).equals(v)) {
			remove(k);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean replace(K k, V original, V replacement) {
		if (containsKey(k) && get(k).equals(original)) {
			put(k, replacement);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized V replace(K k, V v) {
		if (containsKey(k)) {
			return put(k, v);
		} else {
			return null;
		}
	}

}
