package com.prohori.app.models;

import java.io.Serializable;
import java.util.ArrayList;

/** Result of analysing one app: level, score, the reasons and a recommended action. */
public class RiskResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public String appName = "";
    public String packageName = "";
    public String versionName = "";
    public String installSource = "";
    public String recommendation = "";
    public RiskLevel riskLevel = RiskLevel.SAFE;
    public int riskScore;
    public boolean systemApp;
    public ArrayList<String> reasons = new ArrayList<>();
    public ArrayList<String> sensitivePermissions = new ArrayList<>();
}
