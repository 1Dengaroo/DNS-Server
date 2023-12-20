package dns;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class representing a cache of stored DNS records.
 *
 * @version 1.0
 */
public class DNSCache {
    private HashMap<String, ArrayList<DNSRecord>> cache;

    public DNSCache() {
        cache = new HashMap<>();
    }

    public void addRecord(String key, DNSRecord record) {
        cache.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
    }

    public ArrayList<DNSRecord> getRecords(String key) {
        ArrayList<DNSRecord> records = cache.getOrDefault(key, new ArrayList<>());
        removeExpiredRecords(records);
        return records;
    }

    private void removeExpiredRecords(ArrayList<DNSRecord> records) {
        records.removeIf(this::isExpired);
    }

    private boolean isExpired(DNSRecord record) {
        return Instant.now().isAfter(record.getTimestamp().plusSeconds(record.getTTL()));
    }

    public void cleanup() {
        cache.values().forEach(this::removeExpiredRecords);
    }
}