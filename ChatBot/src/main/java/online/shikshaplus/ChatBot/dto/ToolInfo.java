package online.shikshaplus.ChatBot.dto;

public class ToolInfo {
    private String id;
    private String name;
    private String description;
    private String icon;
    private int estimatedTokenCost;
    private boolean enabledByDefault;

    public ToolInfo() {
    }

    public ToolInfo(String id, String name, String description, String icon, int estimatedTokenCost, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.estimatedTokenCost = estimatedTokenCost;
        this.enabledByDefault = enabledByDefault;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getEstimatedTokenCost() {
        return estimatedTokenCost;
    }

    public void setEstimatedTokenCost(int estimatedTokenCost) {
        this.estimatedTokenCost = estimatedTokenCost;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }
}
