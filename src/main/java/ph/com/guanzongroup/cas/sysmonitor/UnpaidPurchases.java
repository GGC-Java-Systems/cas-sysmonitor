/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.sysmonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Administrator
 */
public class UnpaidPurchases implements iSystemMonitor {
    private String psMonitorName = "Unpaid Purchases";
    private GRiderCAS poDriver;
    private String[] pasBranchCD;
    private String[] pasCompnyID;
    private String[] pasIndstCdx;
    private String[] pasCategrCd;
    
    JSONArray poJAData = null;
    
    @Override
    public void setDriver(GRiderCAS driver) {
        poDriver = driver;
    }

    @Override
    public String getName() {
        return psMonitorName;
    }

    @Override
    public void setBranchFilter(String[] branchcd) {
        pasBranchCD = branchcd;
    }

    @Override
    public void setCompanyFilter(String[] companycd) {
        pasCompnyID = companycd;
    }

    @Override
    public void setIndustryFilter(String[] indstcd) {
        pasIndstCdx = indstcd;
    }

    @Override
    public void setCategoryFilter(String[] categcd) {
        pasCategrCd = categcd;
    }

    @Override
    public JSONObject processMonitor() {
        String lsSQL;
        JSONObject oRes = new JSONObject();
        
        pasBranchCD = new String[]{poDriver.getBranchCode()};
        lsSQL = "SELECT" + 
                       "  a.sTransNox" + 
                       ", a.dTransact" + 
                       ", c.sCompnyNm" + 
                       ", b.sBranchNm" + 
                       ", d.sCompnyNm" + 
                       ", a.sIndstCdx" + 
                       ", a.sCategrCd" + 
                       ", a.cPurposex" +  
                       ", a.cProcessd" +
                       ", a.dDueDatex" + 
                       ", a.cTranStat" +
                       ", CONCAT(a.sTransNox ,' - ',a.dTransact) sDisplayNme" +
                       ", CONCAT(b.`sBranchNm`, ' - #',a.`sReferNox`) sToolTipx" +
               " FROM PO_Receiving_Master a" +
                    " LEFT JOIN Branch b ON a.sBranchCd = b.sBranchCD" +
                    " LEFT JOIN Client_Master c ON a.sSupplier = c.sClientID" +
                    " LEFT JOIN Company d ON a.sCompnyID = d.sCompnyID" +
               " WHERE a.cTranStat IN ('1', '2')" +
                 " AND a.cPurposex IN ('0', '2')";

      
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
            lsFilterAll += " AND a.sIndstCdx IN(" + lsFilter.substring(2) + ")";
        }

        //set filter by category
        lsFilter = "";
        if (pasCategrCd != null) {
            for (String lsValue : pasCategrCd) {
                lsFilter += ", " + SQLUtil.toSQL(lsValue);
            }
        }
        if (!lsFilter.isEmpty()) {
            lsFilterAll += " AND a.sCategrCd IN(" + lsFilter.substring(2) + ")";
        }

        //set filter by company
        lsFilter = "";
        if (pasCompnyID != null) {
            for (String lsValue : pasCompnyID) {
                lsFilter += ", " + SQLUtil.toSQL(lsValue);
            }
        }
        if (!lsFilter.isEmpty()) {
            lsFilterAll += " AND a.sCompnyID IN(" + lsFilter.substring(2) + ")";
        }

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
//            System.out.println("Monitoring Query is = " + lsSQL);
            lsSQL = lsSQL + " ORDER BY a.dTransact ASC ";
            ResultSet loRS = poDriver.executeQuery(lsSQL);

            poJAData = MiscUtil.RS2JSON(loRS);

        } catch (SQLException ex) {
            oRes.put("result", "Failed");
            oRes.put("message", MiscUtil.getException(ex));
            return oRes;
        }

        oRes.put("result", "Success");
        return oRes;
    }

    @Override
    public JSONArray getRecords() {
        return poJAData;
    }
    
    
}
