package ph.com.guanzongroup.cas.sysmonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SystemMonitorMenu {

    private GRiderCAS poDriver;
    private String psProductID;
    private String[] pasBranchCD;
    private String[] pasCompnyID;
    private String[] pasIndstCdx;
    private String[] pasCategrCd;
    private String psIndstCdx;
    private String psCategrCd;
    private ResultSet prsMonitoring;
    private int pnTotalCount = 0;

    public SystemMonitorMenu(GRiderCAS appDriver, String productID) {
        this.poDriver = appDriver;
        this.psProductID = productID;
    }
    public JSONObject processMonitoring() {
        JSONObject loJSON = new JSONObject();

        if (psIndstCdx.equals(System.getProperty("sys.main.industry"))) {
            psCategrCd = "";
        }

        String lsSQL = "SELECT a.sSysMnuCd"
                + ", a.sProdctID"
                + ", a.sSysMdlCd"
                + ", a.nMnuOrder"
                + ", a.sMenuName"
                + ", a.sDescript"
                + ", a.sMenuGrpx"
                + ", a.sMenuCDxx"
                + ", a.sSysMType"
                + ", a.sRemarksx"
                + ", a.cRecdStat"
                + ", b.sIndstCdx"
                + ", b.sCategrCd"
                + " FROM xxxSysMenuMonitor a"
                + " LEFT JOIN xxxSysMenuOthers b ON a.sMenuCDxx = b.sMenuCDxx " 
                + " AND b.sIndstCdx = " + SQLUtil.toSQL(psIndstCdx)
                + (!psCategrCd.isEmpty() ? " AND b.sCategrCd = " + SQLUtil.toSQL(psCategrCd) : "")
                + " LEFT JOIN Industry c ON b.sIndstCdx = c.sIndstCdx"
                + " LEFT JOIN Category d"
                + " ON b.sCategrCd = d.sCategrCd"
                + " WHERE a.sProdctID =  " + SQLUtil.toSQL(psProductID)
                + " AND a.sSysMdlCD IN ("
                + " SELECT"
                + " a.sSysMdlCD"
                + " FROM xxxSysModules a"
                + ", xxxSysModulesProduct b"
                + ", xxxSysModulesIndustry c"
                + " LEFT JOIN Industry d ON c.sIndstCdx = d.sIndstCdx"
                + " WHERE a.sSysMdlCD = b.sSysMdlCD"
                + " AND a.sSysMdlCD = c.sSysMdlCD"
                + " AND c.sIndstCdx = " + SQLUtil.toSQL(psIndstCdx)
                + " AND a.cRecdStat = '1'"
                + ")";
        String lsFilterAll = "";
        String lsFilter;
        //set filter by industry
        lsFilter = "";
        if (pasIndstCdx != null) {
            for (String lsValue : pasIndstCdx) {
                lsFilter += ", " + SQLUtil.toSQL(lsValue);
            }
        }
        if (!lsFilter.isEmpty()) {
            lsFilterAll += " AND b.sIndstCdx IN(" + lsFilter.substring(2) + ")";
        }
        //set filter by category
        lsFilter = "";
        if (pasCategrCd != null) {
            for (String lsValue : pasCategrCd) {
                lsFilter += ", " + SQLUtil.toSQL(lsValue);
            }
        }
        if (!lsFilter.isEmpty()) {
            lsFilterAll += " AND b.sCategrCd IN(" + lsFilter.substring(2) + ")";
        }

//        //set filter by company
//        lsFilter = "";
//        if (pasCompnyID != null) {
//            for (String lsValue : pasCompnyID) {
//                lsFilter += ", " + SQLUtil.toSQL(lsValue);
//            }
//        }
//        if (!lsFilter.isEmpty()) {
//            lsFilterAll += " AND a.sCompnyID IN(" + lsFilter.substring(2) + ")";
//        }

        //set filter by branch
        lsFilter = "";
        if (pasBranchCD != null) {
            for (String lsValue : pasBranchCD) {
                lsFilter += ", " + SQLUtil.toSQL(lsValue);
            }
        }
        if (!lsFilter.isEmpty()) {
            lsFilterAll += " AND a.sBranchCD IN(" + lsFilter.substring(2) + ")";
        }

        if (!lsFilterAll.isEmpty()) {
            lsSQL += lsFilterAll;
        }
        try {
//            System.out.println("System Monitor Menu Query is = " + lsSQL);
            prsMonitoring = poDriver.executeQuery(lsSQL);

            JSONArray loResultJSONArray = new JSONArray();
            Map<String, JSONObject> loMenuMap = new LinkedHashMap<>();

            if (MiscUtil.RecordCount(prsMonitoring) <= 0) {
                loJSON.put("result", "error");
                loJSON.put("message", "No record found!");
                return loJSON;

            }
            // STEP 1: Build all menu nodes
            while (prsMonitoring.next()) {
                JSONObject node = new JSONObject();
                node.put("sSysMnuCd", prsMonitoring.getString("sSysMnuCd"));
                node.put("sProdctID", prsMonitoring.getString("sProdctID"));
                node.put("sSysMdlCd", prsMonitoring.getString("sSysMdlCd"));
                node.put("nMnuOrder", prsMonitoring.getString("nMnuOrder"));
                node.put("sMenuName", prsMonitoring.getString("sMenuName"));
                node.put("sDescript", prsMonitoring.getString("sDescript"));
                node.put("sMenuGrpx", prsMonitoring.getString("sMenuGrpx"));
                node.put("sMenuCDxx", prsMonitoring.getString("sMenuCDxx"));
                node.put("sSysMType", prsMonitoring.getString("sSysMType"));
                node.put("sIndstCdx", prsMonitoring.getString("sIndstCdx"));
                node.put("sCategrCd", prsMonitoring.getString("sCategrCd"));
                node.put("subModule", new JSONArray());
                loMenuMap.put(prsMonitoring.getString("sSysMnuCd"), node);
            }

            // STEP 2: Build parent-child structure
            for (JSONObject node : loMenuMap.values()) {
                String lsMenuGroup = (String) node.get("sMenuGrpx");
                if (lsMenuGroup == null || lsMenuGroup.isEmpty()) {
                    loResultJSONArray.add(node);
                } else {
                    JSONObject loParentNode = loMenuMap.get(lsMenuGroup);
                    if (loParentNode != null) {
                        ((JSONArray) loParentNode.get("subModule")).add(node);
                    } else {
                        loResultJSONArray.add(node);
                    }
                }
            }

            // STEP 3: Load monitor data recursively
            JSONArray filtered = new JSONArray();
            for (Object obj : loResultJSONArray) {
                JSONObject node = (JSONObject) obj;
                if (addMonitorSubModuleRecursive(node)) {
                    filtered.add(node);
                }
            }

            loJSON.put("result", "success");
            loJSON.put("data", filtered);
            return loJSON;

        } catch (SQLException ex) {
            loJSON.put("result", "error");
            loJSON.put("message", MiscUtil.getException(ex));
            return loJSON;
        }
    }

    // -----------------------------
    //  Recursive monitor expansion + pruning
    // -----------------------------
    private boolean addMonitorSubModuleRecursive(JSONObject node) {
        JSONArray subModule = (JSONArray) node.get("subModule");
        JSONArray keptChildren = new JSONArray();

        // Process children first
        if (subModule != null) {
            for (Object o : subModule) {
                JSONObject child = (JSONObject) o;
                if (addMonitorSubModuleRecursive(child)) {
                    keptChildren.add(child);
                }
            }
        }
        node.put("subModule", keptChildren);

        // Get transaction records for this node
        String sSysMType = (String) node.get("sSysMType");
        JSONArray monitorRecords = new JSONArray();
        if (sSysMType != null && !sSysMType.isEmpty()) {
            monitorRecords = getRecords(sSysMType);
            if (monitorRecords == null) {
                monitorRecords = new JSONArray();
            }
        }

        node.put("transaction", monitorRecords);
        node.put("txnCount", monitorRecords.size());

        // Update sMenuName directly with count
        String sMenuName = (String) node.get("sMenuName");
        int subCount = keptChildren.size();
        int txnCount = monitorRecords.size();

        if (subCount > 0) {
            node.put("sMenuName", sMenuName + " (" + subCount + ")");
        } else if (txnCount > 0) {
            node.put("sMenuName", sMenuName + " (" + txnCount + ")");
        }

        pnTotalCount = pnTotalCount + txnCount;
        // Keep only if node has children or transactions
        return (subCount > 0) || (txnCount > 0);
    }

    // -----------------------------
    //  Instantiate and run monitor
    // -----------------------------
    public JSONArray getRecords(String monitorClassName) {
        try {
            String fullName = "ph.com.guanzongroup.cas.sysmonitor." + monitorClassName;
            Class<?> clazz = Class.forName(fullName);
            Object instance = clazz.newInstance();

            if (instance instanceof iSystemMonitor) {
                iSystemMonitor monitor = (iSystemMonitor) instance;
                monitor.setDriver(poDriver);
                if (pasBranchCD != null) {
                    monitor.setBranchFilter(pasBranchCD);
                }
                if (pasCompnyID != null) {
                    monitor.setCompanyFilter(pasCompnyID);
                }
                if (pasIndstCdx != null) {
                    monitor.setIndustryFilter(pasIndstCdx);
                }
                if (pasCategrCd != null) {
                    monitor.setCategoryFilter(pasCategrCd);
                }

                monitor.processMonitor();
                JSONArray records = monitor.getRecords();

                // Ensure it's never null
                return (records != null) ? records : new JSONArray();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new JSONArray();
    }

    // -----------------------------
    //  Filters
    // -----------------------------
    public void setDriver(GRiderCAS driver) {
        this.poDriver = driver;
    }

    public void setBranchFilter(String[] branchcd) {
        this.pasBranchCD = branchcd;
    }

    public void setCompanyFilter(String[] companycd) {
        this.pasCompnyID = companycd;
    }

    public void setIndustryFilter(String[] indstcd) {
        this.pasIndstCdx = indstcd;
    }

    public void setCategoryFilter(String[] categcd) {
        this.pasCategrCd = categcd;
    }
    
    public void setIndustryCode(String indstcd) {
        this.psIndstCdx = indstcd;
    }

    public void setCategoryCode(String categcd) {
        this.psCategrCd = categcd;
    }

    public int getMonitoringCount() {

        return pnTotalCount;
    }
}
