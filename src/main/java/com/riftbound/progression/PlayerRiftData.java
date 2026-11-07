package com.riftbound.progression;

public class PlayerRiftData {
    public int unlockedTier = 0;
    public int sentinelKills = 0;
    public int dashCooldown = 0;
    public int pulseCooldown = 0;
    public int shieldCooldown = 0;
    public int shieldActiveTicks = 0;

    public int getUnlockedTier() { return unlockedTier; }
    public void setUnlockedTier(int tier) { this.unlockedTier = tier; }

    public int getSentinelKills() { return sentinelKills; }
    public void setSentinelKills(int kills) { this.sentinelKills = kills; }

    public int getDashCooldown() { return dashCooldown; }
    public void setDashCooldown(int cd) { this.dashCooldown = cd; }

    public int getPulseCooldown() { return pulseCooldown; }
    public void setPulseCooldown(int cd) { this.pulseCooldown = cd; }

    public int getShieldCooldown() { return shieldCooldown; }
    public void setShieldCooldown(int cd) { this.shieldCooldown = cd; }

    public int getShieldActiveTicks() { return shieldActiveTicks; }
    public void setShieldActiveTicks(int ticks) { this.shieldActiveTicks = ticks; }
}