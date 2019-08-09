package com.dotcompliance.limologs.data;

public class DriverInfo {
    public int driver_id;
    public int company_id;
    public String firstname, lastname, email, license,pc_status,ym_status;
    public CompanyInfo company;

    public Boolean isDriver = true;

    public class CompanyInfo {
        public String company_name;
        public String email;
        public String carrier_name, carrier_address, home_terminal;
        public String timezone = "GMT+00:00";
    }

    public DriverInfo() {
        driver_id = 0;
        company = new CompanyInfo();
    }
}