package com.tank.skleaderboards;

import java.util.List;

public final class VariableSource implements ValueSource {
    
    private final String path;

    public VariableSource(String path) {
        this.path = path;
    }

    @Override
    public List<LeaderboardEntry> snapshot() {
        return VariableReader.read(path);
    }

    @Override
    public String describe() {
        return "list variable {" + path + "}";
    }
}
