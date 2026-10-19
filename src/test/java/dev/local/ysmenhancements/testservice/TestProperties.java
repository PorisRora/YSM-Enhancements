package dev.local.ysmenhancements.testservice;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public final class TestProperties implements IGlobalPropertyService {
    private final Map<IPropertyKey, Object> properties = new HashMap<>();
    private record Key(String name) implements IPropertyKey {}
    @Override public IPropertyKey resolveKey(String name) { return new Key(name); }
    @SuppressWarnings("unchecked")
    @Override public <T> T getProperty(IPropertyKey key) { return (T) properties.get(key); }
    @Override public void setProperty(IPropertyKey key, Object value) { properties.put(key, value); }
    @Override public <T> T getProperty(IPropertyKey key, T fallback) { T value = getProperty(key); return value == null ? fallback : value; }
    @Override public String getPropertyString(IPropertyKey key, String fallback) { Object value = properties.get(key); return value == null ? fallback : value.toString(); }
}
