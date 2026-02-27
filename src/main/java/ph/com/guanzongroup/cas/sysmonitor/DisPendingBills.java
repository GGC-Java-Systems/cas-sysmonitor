/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.sysmonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 02-27-2026
 */
public class DisPendingBills implements iSystemMonitor {
    private String psMonitorName = "Pending Bills";
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
        
        lsSQL = " SELECT        "
                + "   a.sTransNox "
                + " , a.sBatchNox "
                + " , a.sRecurrNo "
                + " , a.sSourceCD "
                + " , a.nBillMnth "
                + " , b.nBillDayx "
                + " , b.nDueDayxx "
                + " , b.nAmountxx "
                + " , g.sIndstCdx "
                + " , d.sPayeeNme AS xPayeeNme "
                + " , e.sDescript AS xParticlr "
                + " , g.sDescript AS xIndustry "
                + " , h.sBranchNm AS xBranchNm "
                + " , f.sCompnyNm AS xEmployNm "
                + " , SUM(b.nAmountxx) AS xAmountxx " 
                + " , GROUP_CONCAT(a.sTransNox) AS sTransNox"
                + " , CONCAT(h.sBranchNm, ' : ',d.sPayeeNme, '-' , e.sDescript ,' - ',"
                + "     ELT(a.nBillMnth, 'January','February','March','April','May','June','July','August','September','October','November','December') "
                + " , '- ', b.nDueDayxx) sDisplayNme" 
                + " , 'Payment Request - New' sToolTipx" 
                + " FROM Recurring_Expense_Payment_Monitor a "
                + " LEFT JOIN Recurring_Expense_Schedule b ON a.sRecurrNo = b.sRecurrNo AND b.cRecdStat = '1'  "
                + " LEFT JOIN Recurring_Expense c ON c.sRecurrID = b.sRecurrID AND c.cRecdStat = '1'           "
                + " LEFT JOIN Payee d ON d.sPayeeIDx = b.sPayeeIDx         "
                + " LEFT JOIN Particular e ON e.sPrtclrID = c.sPrtclrID    "
                + " LEFT JOIN Client_Master f ON f.sClientID = b.sEmployID "
                + " LEFT JOIN Industry g ON g.sIndstCdx = c.sIndstCdx      "
                + " LEFT JOIN Branch h ON h.sBranchCd = b.sBranchCd       " ;
         //she ->Status to be change nalang after finalization
        
        if(!poDriver.isMainOffice()){
            lsSQL = MiscUtil.addCondition(lsSQL, " (sBatchNox IS NULL OR TRIM(sBatchNox) = '') AND b.cAccntble = " + SQLUtil.toSQL("1")); //For Branch Only
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, " (sBatchNox IS NULL OR TRIM(sBatchNox) = '') AND b.cAccntble != " + SQLUtil.toSQL("1")); //Except Branch
        }
        
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
            lsFilterAll += " AND c.sIndstCdx IN(" + lsFilter.substring(2) + ")";
        }

        if (!lsFilterAll.isEmpty()) {
            lsSQL += lsFilterAll;
        }
        
        lsSQL = lsSQL + " GROUP BY b.sPayeeIDx, b.sBranchCd, c.sPrtclrID, b.nDueDayxx ";
        
        try {
            System.out.println("Monitoring Query is = " + lsSQL);
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
