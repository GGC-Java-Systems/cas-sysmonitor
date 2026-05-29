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
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
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
                + " , b.cAccntble "
                + " , h.sCompnyID "
                + " , d.sPayeeNme AS xPayeeNme "
                + " , e.sDescript AS xParticlr "
                + " , g.sDescript AS xIndustry "
                + " , h.sBranchNm AS xBranchNm "
                + " , f.sCompnyNm AS xEmployNm "
                + " , SUM(b.nAmountxx) AS xAmountxx " 
                + " , GROUP_CONCAT(a.sTransNox) AS sTransNox"
//                + " , CONCAT(h.sBranchNm, ' : ',d.sPayeeNme, '-' , e.sDescript ,' - '," // Removed branch in display since list is not grouped per branch - Arsiela 05-28-2026 01:38 PM
                + " , CONCAT(d.sPayeeNme, ' : ' , e.sDescript ,' - ',"
                + "     ELT(a.nBillMnth, 'January','February','March','April','May','June','July','August','September','October','November','December') "
                + " , '- ', b.nDueDayxx) sDisplayNme" 
                + " , 'Payment Requests - New' sToolTipx" 
                + " FROM Recurring_Expense_Payment_Monitor a "
                + " INNER JOIN Recurring_Expense_Schedule b ON a.sRecurrNo = b.sRecurrNo AND b.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " LEFT JOIN Recurring_Expense c ON c.sRecurrID = b.sRecurrID AND c.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " LEFT JOIN Payee d ON d.sPayeeIDx = b.sPayeeIDx         "
                + " LEFT JOIN Particular e ON e.sPrtclrID = c.sPrtclrID    "
                + " LEFT JOIN Client_Master f ON f.sClientID = b.sEmployID "
                + " LEFT JOIN Industry g ON g.sIndstCdx = c.sIndstCdx      "
                + " LEFT JOIN Branch h ON h.sBranchCd = b.sBranchCd       " 
                + " LEFT JOIN Company i ON i.sCompnyID = h.sCompnyID       " ;
         //she ->Status to be change nalang after finalization
        
        lsSQL = MiscUtil.addCondition(lsSQL, " (a.sBatchNox IS NULL OR TRIM(a.sBatchNox) = '') AND b.cExcluded = " + SQLUtil.toSQL(Logical.NO));
        if(poDriver.isMainOffice()){
            lsSQL = lsSQL + " AND b.cAccntble != " + SQLUtil.toSQL(Logical.YES); //Except Accountable is equal to Branch : 1
        } else {
            lsSQL = lsSQL + " AND b.sBranchCd = "  + SQLUtil.toSQL(poDriver.getBranchCode()); //For Specific Branch Only
        }
        
        String lsFilterAll = "";
        String lsFilter;
     
////        set filter by industry
//        lsFilter = "";
//        if (pasIndstCdx != null) { //Never pang na lagyan ito pasIndstCdx ng value; as per sir maynard kasi ni as is palang muna yung pag filter dapat mag filter pa sa lahat ng industry;
//            for (String lsValue : pasIndstCdx) {
//                lsFilter += ", " + SQLUtil.toSQL(lsValue);
//            }
//        }
//        if (!lsFilter.isEmpty()) {
//            lsFilterAll += " AND c.sIndstCdx IN(" + lsFilter.substring(2) + ")";
//        }

        lsFilter = "";
        
        //Filter by Company based of current logged in; filter by branch company - request by ma'am she : Arsiela 05-28-2026 
        if (pasCompnyID != null) {
            for (String lsValue : pasCompnyID) {
                lsFilter += ", " + SQLUtil.toSQL(lsValue);
            }
        }
        if (!lsFilter.isEmpty()) {
            lsFilterAll += " AND h.sCompnyID IN(" + lsFilter.substring(2) + ")";
        }

        if (!lsFilterAll.isEmpty()) {
            lsSQL += lsFilterAll;
        }
        
//        lsSQL = lsSQL + " GROUP BY b.sPayeeIDx, b.sBranchCd, c.sPrtclrID, a.nBillMnth, b.nDueDayxx ";
        lsSQL = lsSQL + " GROUP BY h.sCompnyID, b.sPayeeIDx, b.cAccntble, b.nBillDayx "; //Changed group by according to ma'am she - Arsiela 05-28-206
        
        try {
//            System.out.println("Monitoring Query is = " + lsSQL);
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
