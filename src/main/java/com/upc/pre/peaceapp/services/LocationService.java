package com.upc.pre.peaceapp.services;

import com.upc.pre.peaceapp.models.Location;
import com.upc.pre.peaceapp.repositories.LocationRepository;
import com.upc.pre.peaceapp.schemas.LocationSchema;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {
    private final LocationRepository repository;

    public LocationService(LocationRepository repository) {
        this.repository = repository;
    }

    public Location saveLocation(LocationSchema location) {
        return repository.save(new Location(location.latitude(), location.longitude(), location.idReport()));
    }

    public List<Location> getAllLocations() {
        return repository.findAll();
    }

    public List<Location> obtainDangerousLocations(int quantityReports) {
        List<Location> locations = repository.findAll();
        Map<Pair<String, String>, Integer> m = new HashMap<>();

        for (Location location : locations) {
            Pair<String, String> pair = new Pair<>(location.getALatitude(), location.getALongitude());

            if (m.containsKey(pair)) {
                m.put(pair, m.get(pair) + 1);
            } else {
                m.put(pair, 1);
            }
        }

        // sort descendent m
        m = m.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(HashMap::new, (m1, e) -> m1.put(e.getKey(), e.getValue()), Map::putAll);


        for(Map.Entry<Pair<String, String>, Integer> entry : m.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        return locations;

    }
}
