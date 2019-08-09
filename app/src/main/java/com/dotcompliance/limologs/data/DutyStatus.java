package com.dotcompliance.limologs.data;

public class DutyStatus {
    public static final int STATUS_OFF = 0;
    public static final int STATUS_SLEEPER = 1; // Sleeper Berth
    public static final int STATUS_DRIVING = 2; // Driving
    public static final int STATUS_ON = 3;
    public static final int STATUS_PC = 4;
    public static final int STATUS_YM = 5;
    public static final int STATUS_LOGIN = 6;
    public static final int STATUS_LOGOUT =7;
    public static final int STATUS_CERTIFIED = 8;



    public int Id = 0;
    public int status = STATUS_OFF;
    public int start_time;  // minutes of day, 0..1439
    public int end_time;
    public String location;
    public String remark;

    public DutyStatus() {
        Id = 0;
        status = STATUS_OFF;
        start_time = 0;
        end_time = 0;
        location = "";
        remark = "";
    }

    public DutyStatus(DutyStatus other) {
        Id = other.Id;
        status = other.status;
        start_time = other.start_time;
        end_time = other.end_time;
        location = other.location;
        remark = other.remark;
    }

    public DutyStatus(int _id, int s, int st, int et, String loc, String rem) {
        Id = _id;
        status = s;
        start_time = st;
        end_time = et;
        location = loc;
        remark = rem;
    }

    public String getStatusString() {
        String str = "";
        switch (status) {
            case STATUS_OFF:
                str = "Off Duty";
                break;
            case STATUS_SLEEPER:
                str = "Sleeper";
                break;
            case STATUS_DRIVING:
                str = "Driving";
                break;
            case STATUS_ON:
                str = "On Duty";
                break;
            case STATUS_PC:
                str = "Personal Conveyance";
                break;

            case STATUS_YM:
                str = "Yard Move";
                break;

            case STATUS_LOGIN:
                str = "Login";
                break;

            case STATUS_LOGOUT:
                str = "Logout";
                break;


            case STATUS_CERTIFIED:
                str = "Certifed";
                break;



        }
        return str;
    }

    public int getNormalizeStatus() {

        switch (status) {

            case STATUS_OFF:
            case STATUS_PC:
            case STATUS_LOGIN:
            case STATUS_LOGOUT:

                return STATUS_OFF; // 0, 4, 6, 7 => 0


            case STATUS_ON:
            case STATUS_YM:
            case STATUS_CERTIFIED:
            case 9:
            case 10:

                return STATUS_ON; // 3, 5, 8, 9, 10 => 3

        }

        return status;
    }

    public void setStart_time(int s)
    {
        start_time = s;
    }

    public static DutyStatus copy(DutyStatus example) {
        return new DutyStatus(example);
    }
}