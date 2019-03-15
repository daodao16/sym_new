package org.b3log.symphony.model;

/**
 * This class defines user job model relevant keys.
 * @author daodao
 *
 */
public final class UserJob {
	/**
     * user_job.
     */
    public static final String USER_JOB = "user_job";
    
    /**
     * Key of user job id.
     */
    public static final String USER_JOB_T_ID = "userJobId";
    
    /**
     * Key of company name.
     */
    public static final String COMPANY_NAME = "companyName";
    
    /**
     * Key of job code.
     */
    public static final String JOB_CODE = "jobCode";
    
    /**
     * Key of job type.
     */
    public static final String JOB_TYPE = "jobType";
    
    /**
     * Key of leave type.
     */
    public static final String LEAVE_TYPE = "leaveType";
    
    /**
     * Key of leave period.
     */
    public static final String LEAVE_PERIOD = "leavePeriod";
    
    /**
     * Key of leave reason.
     */
    public static final String LEAVE_REASON = "leaveReason";
    
    /**
     * Key of create time.
     */
	public static final String CREATE_TIME = "createTime";
    
    /**
     * job type - now on job
     */
    public static final String JOB_TYPE_ON = "O";
    
    /**
     * job type - now leave
     */
    public static final String JOB_TYPE_LEAVE = "L";
    
    /**
     * leave type-now on job
     */
    public static final int LEAVE_TYPE_NOT_LEAVE = 0;
    
    /**
     * leave type-active
     */
    public static final int LEAVE_TYPE_ACTIVE = 1;
    
    /**
     * leave type-inactive
     */
    public static final int LEAVE_TYPE_INACTIVE = 2;
}
