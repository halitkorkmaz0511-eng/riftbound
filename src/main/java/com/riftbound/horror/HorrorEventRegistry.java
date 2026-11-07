package com.riftbound.horror;
import com.riftbound.horror.event.HorrorEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class HorrorEventRegistry {
    private static final Map<String, HorrorEvent> EVENTS = new HashMap<>();

    public static void register(HorrorEvent event) { EVENTS.put(event.getId(), event); }
    public static HorrorEvent getEvent(String id) { return EVENTS.get(id); }
    public static Collection<HorrorEvent> getAll() { return EVENTS.values(); }
}