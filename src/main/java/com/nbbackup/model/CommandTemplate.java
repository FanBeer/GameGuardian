package com.nbbackup.model;

public class CommandTemplate {
    private int id;
    private int brandId;
    private String brandName;
    private String commandType;
    private String command;
    private String description;

    public CommandTemplate() {}

    public CommandTemplate(int brandId, String brandName, String commandType, String command, String description) {
        this.brandId = brandId;
        this.brandName = brandName;
        this.commandType = commandType;
        this.command = command;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBrandId() { return brandId; }
    public void setBrandId(int brandId) { this.brandId = brandId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
