package com.nbbackup.model;

public class Brand {
    private int id;
    private String name;
    private String displayName;
    private String description;

    public Brand() {}

    public Brand(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public Brand(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return displayName;
    }
}
