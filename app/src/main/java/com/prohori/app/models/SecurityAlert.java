package com.prohori.app.models;

import java.io.Serializable;

/** A stored security alert (High or Critical finding). */
public class SecurityAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String appName = "";
    public String packageName = "";
    public RiskLevel riskLevel = RiskLevel.HIGH;
    public int riskScore;
    public String description = "";
    public String recommendation = "";
    public long timestamp;
    public boolean isRead;
}
