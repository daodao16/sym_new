/*
 * Symphony - A modern community (forum/BBS/SNS/blog) platform written in Java.
 * Copyright (C) 2012-2019, b3log.org & hacpai.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.b3log.symphony.processor;

import com.qiniu.util.Auth;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.Latkes.RuntimeMode;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.model.User;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.servlet.HttpMethod;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.latke.servlet.annotation.After;
import org.b3log.latke.servlet.annotation.Before;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.AbstractFreeMarkerRenderer;
import org.b3log.latke.util.Locales;
import org.b3log.latke.util.Requests;
import org.b3log.symphony.model.*;
import org.b3log.symphony.processor.advice.CSRFToken;
import org.b3log.symphony.processor.advice.LoginCheck;
import org.b3log.symphony.processor.advice.PermissionGrant;
import org.b3log.symphony.processor.advice.stopwatch.StopwatchEndAdvice;
import org.b3log.symphony.processor.advice.stopwatch.StopwatchStartAdvice;
import org.b3log.symphony.processor.advice.validate.UserForgetPwdValidation;
import org.b3log.symphony.processor.advice.validate.UserJobValidation;
import org.b3log.symphony.processor.advice.validate.UserRegister2Validation;
import org.b3log.symphony.processor.advice.validate.UserRegisterValidation;
import org.b3log.symphony.processor.advice.validate.UserMobileValidateValidation;
import org.b3log.symphony.service.*;
import org.b3log.symphony.util.Sessions;
import org.b3log.symphony.util.Symphonys;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login/Register processor.
 * <ul>
 * <li>Registration (/register), GET/POST</li>
 * <li>Login (/login), GET/POST</li>
 * <li>Logout (/logout), GET</li>
 * <li>Reset password (/reset-pwd), GET/POST</li>
 * </ul>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="http://vanessa.b3log.org">Liyuan Li</a>
 * @version 1.13.12.6, Jan 5, 2019
 * @since 0.2.0
 */
@RequestProcessor
public class LoginProcessor {

    /**
     * Wrong password tries.
     * <p>
     * &lt;userId, {"wrongCount": int, "captcha": ""}&gt;
     * </p>
     */
    public static final Map<String, JSONObject> WRONG_PWD_TRIES = new ConcurrentHashMap<>();

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(LoginProcessor.class);

    /**
     * User management service.
     */
    @Inject
    private UserMgmtService userMgmtService;

    /**
     * User query service.
     */
    @Inject
    private UserQueryService userQueryService;

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Pointtransfer management service.
     */
    @Inject
    private PointtransferMgmtService pointtransferMgmtService;

    /**
     * Data model service.
     */
    @Inject
    private DataModelService dataModelService;

    /**
     * Verifycode management service.
     */
    @Inject
    private VerifycodeMgmtService verifycodeMgmtService;

    /**
     * Verifycode query service.
     */
    @Inject
    private VerifycodeQueryService verifycodeQueryService;

    /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;

    /**
     * Invitecode query service.
     */
    @Inject
    private InvitecodeQueryService invitecodeQueryService;

    /**
     * Invitecode management service.
     */
    @Inject
    private InvitecodeMgmtService invitecodeMgmtService;

    /**
     * Invitecode management service.
     */
    @Inject
    private NotificationMgmtService notificationMgmtService;

    /**
     * Role query service.
     */
    @Inject
    private RoleQueryService roleQueryService;

    /**
     * Tag query service.
     */
    @Inject
    private TagQueryService tagQueryService;
    
    /**
     * job query service
     */
    @Inject
    private JobQueryService jobQueryService;
    
    /**
     * user job mangement service
     */
    @Inject
    private UserJobMgmtService userJobMgmtService;
    
    /**
     * user job query service
     */
    @Inject
    private UserJobQueryService userJobQueryService;

    /**
     * Next guide step.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/guide/next", method = HttpMethod.POST)
    @Before({LoginCheck.class})
    public void nextGuideStep(final RequestContext context) {
        context.renderJSON();

        JSONObject requestJSONObject;
        try {
            requestJSONObject = context.requestJSON();
        } catch (final Exception e) {
            LOGGER.warn(e.getMessage());

            return;
        }

        final HttpServletRequest request = context.getRequest();
        JSONObject user = Sessions.getUser();
        final String userId = user.optString(Keys.OBJECT_ID);

        int step = requestJSONObject.optInt(UserExt.USER_GUIDE_STEP);

        if (UserExt.USER_GUIDE_STEP_STAR_PROJECT < step || UserExt.USER_GUIDE_STEP_FIN >= step) {
            step = UserExt.USER_GUIDE_STEP_FIN;
        }

        try {
            user = userQueryService.getUser(userId);
            user.put(UserExt.USER_GUIDE_STEP, step);
            userMgmtService.updateUser(userId, user);
            
            //add by daodao at 20190304 增加引导之后的积分,引导完成之后给分
            if(UserExt.USER_GUIDE_STEP_FIN == step) {
            	pointtransferMgmtService.transfer(Pointtransfer.ID_C_SYS, userId,
                        Pointtransfer.TRANSFER_TYPE_C_FIN_GUIDE,
                        Pointtransfer.TRANSFER_SUM_C_FIN_GUIDE, userId, System.currentTimeMillis(), "");
            }
            
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Guide next step [" + step + "] failed", e);

            return;
        }

        context.renderJSON(true);
    }
    
    /**
     * add job info guide step.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/guide/updateJobInfo", method = HttpMethod.POST)
    @Before({LoginCheck.class,UserJobValidation.class})
    public void jobInfoGuideStep(final RequestContext context) {
        context.renderJSON();
        
        final JSONObject requestJSONObject = (JSONObject) context.attr(Keys.REQUEST);
        
        JSONObject user = Sessions.getUser();
        final String userId = user.optString(Keys.OBJECT_ID);
        
        //处理技能数据
        String skills = requestJSONObject.optString(JobSkill.MAIN_SKILL);
        String[] skillArray = null;
        
        if(StringUtils.isNotEmpty(skills)) {
        	skillArray = skills.split(" ");
        }
        
        try {
        	//修改职位信息
        	JSONObject userJob = userJobQueryService.getLatestUserJobByType(userId, UserJob.JOB_TYPE_LEAVE);
            userJob.put(UserJob.LEAVE_TYPE, requestJSONObject.optInt(UserJob.LEAVE_TYPE));
            userJob.put(UserJob.LEAVE_PERIOD, requestJSONObject.optString(UserJob.LEAVE_PERIOD));
            userJob.put(UserJob.LEAVE_REASON, requestJSONObject.optString(UserJob.LEAVE_REASON));
            userJob.put(UserExt.USER_T_ID, userId);
        	userJobMgmtService.updateUserJob(userJob, skillArray);
        	
        	context.renderTrueResult().renderMsg(langPropsService.get("userJobUpdatedLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Guide job info step failed", e);
            context.renderMsg(e.getMessage());

            return;
        }

    }

    /**
     * Shows guide page.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/guide", method = HttpMethod.GET)
    @Before({StopwatchStartAdvice.class, LoginCheck.class})
    @After({CSRFToken.class, PermissionGrant.class, StopwatchEndAdvice.class})
    public void showGuide(final RequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject currentUser = Sessions.getUser();
        final int step = currentUser.optInt(UserExt.USER_GUIDE_STEP);
        if (UserExt.USER_GUIDE_STEP_FIN == step) {
            context.sendRedirect(Latkes.getServePath());

            return;
        }

        final AbstractFreeMarkerRenderer renderer = new SkinRenderer(context, "verify/guide.ftl");
        final Map<String, Object> dataModel = renderer.getDataModel();
        dataModel.put(Common.CURRENT_USER, currentUser);

        final List<JSONObject> tags = tagQueryService.getTags(32);
        dataModel.put(Tag.TAGS, tags);

        final List<JSONObject> users = userQueryService.getNiceUsers(6);
        final Iterator<JSONObject> iterator = users.iterator();
        while (iterator.hasNext()) {
            final JSONObject user = iterator.next();
            if (user.optString(Keys.OBJECT_ID).equals(currentUser.optString(Keys.OBJECT_ID))) {
                iterator.remove();

                break;
            }
        }
        dataModel.put(User.USERS, users);
        
        //add by daodao at 2019/2/12
        List<JSONObject> leavePeriods = jobQueryService.getAllJobLeavePeriods();
        dataModel.put("leavePeriods", leavePeriods);
        List<JSONObject> leaveReasons = jobQueryService.getAllJobLeaveReasons();
        dataModel.put("leaveReasons", leaveReasons);
        //add over

        // Qiniu file upload authenticate
        final Auth auth = Auth.create(Symphonys.get("qiniu.accessKey"), Symphonys.get("qiniu.secretKey"));
        final String uploadToken = auth.uploadToken(Symphonys.get("qiniu.bucket"));
        dataModel.put("qiniuUploadToken", uploadToken);
        dataModel.put("qiniuDomain", Symphonys.get("qiniu.domain"));

        if (!Symphonys.getBoolean("qiniu.enabled")) {
            dataModel.put("qiniuUploadToken", "");
        }

        final long imgMaxSize = Symphonys.getLong("upload.img.maxSize");
        dataModel.put("imgMaxSize", imgMaxSize);
        final long fileMaxSize = Symphonys.getLong("upload.file.maxSize");
        dataModel.put("fileMaxSize", fileMaxSize);

        dataModelService.fillHeaderAndFooter(context, dataModel);
    }

    /**
     * Shows login page.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/login", method = HttpMethod.GET)
    @Before(StopwatchStartAdvice.class)
    @After({PermissionGrant.class, StopwatchEndAdvice.class})
    public void showLogin(final RequestContext context) {
        if (Sessions.isLoggedIn()) {
            context.sendRedirect(Latkes.getServePath());

            return;
        }

        String referer = context.param(Common.GOTO);
        if (StringUtils.isBlank(referer)) {
            referer = context.header("referer");
        }

        if (!StringUtils.startsWith(referer, Latkes.getServePath())) {
            referer = Latkes.getServePath();
        }

        final AbstractFreeMarkerRenderer renderer = new SkinRenderer(context, "verify/login.ftl");
        final Map<String, Object> dataModel = renderer.getDataModel();
        dataModel.put(Common.GOTO, referer);

        dataModelService.fillHeaderAndFooter(context, dataModel);
    }

    /**
     * Shows forget password page.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/forget-pwd", method = HttpMethod.GET)
    @Before(StopwatchStartAdvice.class)
    @After({PermissionGrant.class, StopwatchEndAdvice.class})
    public void showForgetPwd(final RequestContext context) {
        final AbstractFreeMarkerRenderer renderer = new SkinRenderer(context, "verify/forget-pwd.ftl");
        final Map<String, Object> dataModel = renderer.getDataModel();
        dataModelService.fillHeaderAndFooter(context, dataModel);
    }

    /**
     * Forget password.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/forget-pwd", method = HttpMethod.POST)
    @Before(UserForgetPwdValidation.class)
    public void forgetPwd(final RequestContext context) {
        context.renderJSON();

        //final HttpServletRequest request = context.getRequest();
        final JSONObject requestJSONObject = (JSONObject) context.attr(Keys.REQUEST);
        //final String email = requestJSONObject.optString(User.USER_EMAIL);
        final String mobile = requestJSONObject.optString(UserExt.USER_MOBOLE);
        final String mobileVCode = requestJSONObject.optString(UserExt.USER_MOBOLE_V_CODE);

        //try {
        	//remove by daodao at 20190131
//            final JSONObject user = userQueryService.getUserByEmail(email);
//            if (null == user || UserExt.USER_STATUS_C_VALID != user.optInt(UserExt.USER_STATUS)) {
//                context.renderFalseResult().renderMsg(langPropsService.get("notFoundUserLabel"));
//
//                return;
//            }
//
//            final String userId = user.optString(Keys.OBJECT_ID);
//
//            final JSONObject verifycode = new JSONObject();
//            verifycode.put(Verifycode.BIZ_TYPE, Verifycode.BIZ_TYPE_C_RESET_PWD);
//            final String code = RandomStringUtils.randomAlphanumeric(6);
//            verifycode.put(Verifycode.CODE, code);
//            verifycode.put(Verifycode.EXPIRED, DateUtils.addDays(new Date(), 1).getTime());
//            verifycode.put(Verifycode.RECEIVER, email);
//            verifycode.put(Verifycode.STATUS, Verifycode.STATUS_C_UNSENT);
//            verifycode.put(Verifycode.TYPE, Verifycode.TYPE_C_EMAIL);
//            verifycode.put(Verifycode.USER_ID, userId);
//            verifycodeMgmtService.addVerifycode(verifycode);
        	
        final JSONObject verifycode = verifycodeQueryService.getVerifycodeByReciver(Verifycode.TYPE_C_MOBILE, 
        		Verifycode.BIZ_TYPE_C_RESET_PWD, mobile);
        	
        if(null == verifycode) {
        	context.renderMsg(langPropsService.get("verifycodeExpiredLabel"));
        	return;
        }
        	
        //查看手机验证码是否一致
        if(StringUtils.equals(verifycode.getString(Verifycode.CODE), mobileVCode)) {
        	context.renderTrueResult().renderMsg(langPropsService.get("verifycodeSentLabel"));
        } else {
        	context.renderMsg(langPropsService.get("invalidVCodeLabel"));
        }

//        } catch (final ServiceException e) {
//            final String msg = langPropsService.get("resetPwdLabel") + " - " + e.getMessage();
//            LOGGER.log(Level.ERROR, msg + "[mobile=" + mobile + "]");
//
//            context.renderMsg(msg);
//        }
    }

    /**
     * Shows reset password page.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/reset-pwd", method = HttpMethod.GET)
    @Before(StopwatchStartAdvice.class)
    @After({PermissionGrant.class, StopwatchEndAdvice.class})
    public void showResetPwd(final RequestContext context) {
        final AbstractFreeMarkerRenderer renderer = new SkinRenderer(context, null);
        context.setRenderer(renderer);
        final Map<String, Object> dataModel = renderer.getDataModel();

        final String code = context.param("code");
        final JSONObject verifycode = verifycodeQueryService.getVerifycode(code);
        if (null == verifycode) {
            dataModel.put(Keys.MSG, langPropsService.get("verifycodeExpiredLabel"));
            renderer.setTemplateName("error/custom.ftl");
        } else {
            renderer.setTemplateName("verify/reset-pwd.ftl");

            final String userId = verifycode.optString(Verifycode.USER_ID);
            final JSONObject user = userQueryService.getUser(userId);
            dataModel.put(User.USER, user);
            dataModel.put(Common.CODE, code);
        }

        dataModelService.fillHeaderAndFooter(context, dataModel);
    }

    /**
     * Resets password.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/reset-pwd", method = HttpMethod.POST)
    public void resetPwd(final RequestContext context) {
        context.renderJSON();

        final HttpServletResponse response = context.getResponse();
        final JSONObject requestJSONObject = context.requestJSON();
        final String password = requestJSONObject.optString(User.USER_PASSWORD); // Hashed
        final String userId = requestJSONObject.optString(UserExt.USER_T_ID);
        final String code = requestJSONObject.optString(Common.CODE);
        //remove by daodao at 20190131 这里就不需要校验了，不然过期的风险很大的了
//        final JSONObject verifycode = verifycodeQueryService.getVerifycode(code);
//        if (null == verifycode || !verifycode.optString(Verifycode.USER_ID).equals(userId)) {
//            context.renderMsg(langPropsService.get("verifycodeExpiredLabel"));
//
//            return;
//        }
        
        String name = null;
        String mobile = null;
        //String email = null;
        try {
            final JSONObject user = userQueryService.getUser(userId);
            if (null == user || UserExt.USER_STATUS_C_VALID != user.optInt(UserExt.USER_STATUS)) {
                context.renderMsg(langPropsService.get("resetPwdLabel") + " - " + "User Not Found");

                return;
            }
            
            name = user.optString(User.USER_NAME);
            mobile = user.optString(UserExt.USER_MOBOLE);
            user.put(User.USER_PASSWORD, password);
            userMgmtService.updatePassword(user);
            verifycodeMgmtService.removeByCode(code);
            context.renderTrueResult();
            LOGGER.info("User [mobile=" + mobile + "] reseted password");

            Sessions.login(response, userId, true);
        } catch (final ServiceException e) {
            final String msg = langPropsService.get("resetPwdLabel") + " - " + e.getMessage();
            LOGGER.log(Level.ERROR, msg + "[name={0}, mobile={1}]", name, mobile);

            context.renderMsg(msg);
        }
    }

    /**
     * Shows registration page.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/register", method = HttpMethod.GET)
    @Before(StopwatchStartAdvice.class)
    @After({PermissionGrant.class, StopwatchEndAdvice.class})
    public void showRegister(final RequestContext context) {
        if (Sessions.isLoggedIn()) {
            context.sendRedirect(Latkes.getServePath());

            return;
        }

        final AbstractFreeMarkerRenderer renderer = new SkinRenderer(context, null);
        final Map<String, Object> dataModel = renderer.getDataModel();
        dataModel.put(Common.REFERRAL, "");

        boolean useInvitationLink = false;

        String referral = context.param("r");
        if (!UserRegisterValidation.invalidUserName(referral)) {
            final JSONObject referralUser = userQueryService.getUserByName(referral);
            if (null != referralUser) {
                dataModel.put(Common.REFERRAL, referral);

                final Map<String, JSONObject> permissions =
                        roleQueryService.getUserPermissionsGrantMap(referralUser.optString(Keys.OBJECT_ID));
                final JSONObject useILPermission =
                        permissions.get(Permission.PERMISSION_ID_C_COMMON_USE_INVITATION_LINK);
                useInvitationLink = useILPermission.optBoolean(Permission.PERMISSION_T_GRANT);
            }
        }


        //remove by daodao at 20190128
        //final String code = context.param("code");
        //remove over
        final String userId = context.param("uid");
        renderer.setTemplateName("verify/register.ftl");
        

        if (StringUtils.isBlank(userId)) { // Register Step 1
            renderer.setTemplateName("verify/register.ftl");
        } else { // Register Step 2
//            final JSONObject verifycode = verifycodeQueryService.getVerifycode(code);
//            if (null == verifycode) {
//                dataModel.put(Keys.MSG, langPropsService.get("verifycodeExpiredLabel"));
//                renderer.setTemplateName("error/custom.ftl");
//            } else {
        	renderer.setTemplateName("verify/register2.ftl");

                //final String userId = verifycode.optString(Verifycode.USER_ID);
        	final JSONObject user = userQueryService.getUser(userId);
        	dataModel.put(User.USER, user);

        	if (UserExt.USER_STATUS_C_VALID == user.optInt(UserExt.USER_STATUS)
        			|| UserExt.NULL_USER_NAME.equals(user.optString(User.USER_NAME))) {
                dataModel.put(Keys.MSG, langPropsService.get("userExistLabel"));
                renderer.setTemplateName("error/custom.ftl");
        	}
//                } else {
//                    referral = StringUtils.substringAfter(code, "r=");
//                    if (StringUtils.isNotBlank(referral)) {
//                        dataModel.put(Common.REFERRAL, referral);
//                    }
//                }
        }

        final String allowRegister = optionQueryService.getAllowRegister();
        dataModel.put(Option.ID_C_MISC_ALLOW_REGISTER, allowRegister);
        if (useInvitationLink && "2".equals(allowRegister)) {
            dataModel.put(Option.ID_C_MISC_ALLOW_REGISTER, "1");
        }

        dataModelService.fillHeaderAndFooter(context, dataModel);
    }
    
    @RequestProcessing(value = "/sendMobileVCode", method = HttpMethod.POST)
    @Before(UserMobileValidateValidation.class)
    public void sendMobileVCode(final RequestContext context) {
    	context.renderJSON();

        final JSONObject requestJSONObject = (JSONObject) context.attr(Keys.REQUEST);
        final String mobile = requestJSONObject.optString(UserExt.USER_MOBOLE);
        final int vertifyType = requestJSONObject.optInt(Verifycode.VERTIFY_TYPE);
        
        try {
        	final JSONObject verifycode = new JSONObject();
        	verifycode.put(Verifycode.BIZ_TYPE, vertifyType);
        	String code = null;
        	if(Latkes.getRuntimeMode() == RuntimeMode.PRODUCTION) {
        		code = RandomStringUtils.randomNumeric(6);
        	} else {
        		//开发模式下，取个手机后六位就好了
        		code = StringUtils.right(mobile, 6);
        	}
        
        	verifycode.put(Verifycode.CODE, code);
        	//10分钟过期
        	verifycode.put(Verifycode.EXPIRED, DateUtils.addMinutes(new Date(), 10).getTime());
        	verifycode.put(Verifycode.RECEIVER, mobile);
        	verifycode.put(Verifycode.STATUS, Verifycode.STATUS_C_UNSENT);
        	verifycode.put(Verifycode.TYPE, Verifycode.TYPE_C_MOBILE);
        	
        	JSONObject user = userQueryService.getUserByMobile(mobile);
        	if(null == user) {
        		//不是注册的话，就应该有用户信息，如果为空就是有问题的。
        		if(Verifycode.BIZ_TYPE_C_REGISTER != vertifyType) {
        			context.renderMsg(langPropsService.get("notFoundUserLabel"));
        			return;
        		}
        		verifycode.put(Verifycode.USER_ID, mobile);
        	} else {
        		verifycode.put(Verifycode.USER_ID, user.optString(Keys.OBJECT_ID));
        	}
        	verifycodeMgmtService.sendMobileVerifycode(verifycode);
        	
        	context.renderTrueResult().renderMsg(langPropsService.get("verifycodeSentLabel"));
        }catch (final ServiceException e) {
            final String msg = langPropsService.get("registerFailLabel") + " - " + e.getMessage();
            LOGGER.log(Level.ERROR, msg + "[mobile={0}]", mobile);

            context.renderMsg(msg);
        }
    }
    
    @RequestProcessing(value = "/jobSearch", method = HttpMethod.POST)
    public void searchJobs(final RequestContext context) {
    	context.renderJSON();
    	JSONObject requestJSONObject;
        try {
            requestJSONObject = context.requestJSON();
        } catch (final Exception e) {
            context.renderMsg(langPropsService.get("paramsParseFailedLabel"));

            return;
        }
        final String keyWords = requestJSONObject.optString("keyWords");
        
        List<JSONObject> jobList = jobQueryService.getJobs(keyWords);
        context.renderTrueResult().renderJSONValue("jobData", jobList);
    }

    /**
     * Register Step 1.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/register", method = HttpMethod.POST)
    @Before(UserRegisterValidation.class)
    public void register(final RequestContext context) {
        context.renderJSON();
        //final HttpServletRequest request = context.getRequest();
        final JSONObject requestJSONObject = (JSONObject) context.attr(Keys.REQUEST);
        final String name = requestJSONObject.optString(User.USER_NAME);
        //final String email = requestJSONObject.optString(User.USER_EMAIL);
        final String mobile = requestJSONObject.optString(UserExt.USER_MOBOLE);
        final String invitecode = requestJSONObject.optString(Invitecode.INVITECODE);
        //final String referral = requestJSONObject.optString(Common.REFERRAL);
        final String mobileVCode = requestJSONObject.optString(UserExt.USER_MOBOLE_V_CODE);

        final JSONObject user = new JSONObject();
        user.put(User.USER_NAME, name);
        //user.put(User.USER_EMAIL, email);
        user.put(UserExt.USER_MOBOLE, mobile);
        user.put(User.USER_PASSWORD, "");
        final Locale locale = Locales.getLocale();
        user.put(UserExt.USER_LANGUAGE, locale.getLanguage() + "_" + locale.getCountry());

        try {
        	final JSONObject verifycode = verifycodeQueryService.getVerifycodeByReciver(Verifycode.TYPE_C_MOBILE, 
        			Verifycode.BIZ_TYPE_C_REGISTER, mobile);
        	
        	if(null == verifycode) {
        		context.renderMsg(langPropsService.get("verifycodeExpiredLabel"));
        		return;
        	}
        	
        	//查看手机验证码是否一致
        	if(StringUtils.equals(verifycode.getString(Verifycode.CODE), mobileVCode)) {
        		final String newUserId = userMgmtService.addUser(user);
        		//user.put(Keys.OBJECT_ID, newUserId);

//            final JSONObject verifycode = new JSONObject();
//            verifycode.put(Verifycode.BIZ_TYPE, Verifycode.BIZ_TYPE_C_REGISTER);
//            String code = RandomStringUtils.randomAlphanumeric(6);
//            if (StringUtils.isNotBlank(referral)) {
//                code += "r=" + referral;
//            }
//            verifycode.put(Verifycode.CODE, code);
//            verifycode.put(Verifycode.EXPIRED, DateUtils.addDays(new Date(), 1).getTime());
//            verifycode.put(Verifycode.RECEIVER, email);
//            verifycode.put(Verifycode.STATUS, Verifycode.STATUS_C_UNSENT);
//            verifycode.put(Verifycode.TYPE, Verifycode.TYPE_C_EMAIL);
//            verifycode.put(Verifycode.USER_ID, newUserId);
//            verifycodeMgmtService.addVerifycode(verifycode);

        		final String allowRegister = optionQueryService.getAllowRegister();
        		if ("2".equals(allowRegister) && StringUtils.isNotBlank(invitecode)) {
        			final JSONObject ic = invitecodeQueryService.getInvitecode(invitecode);
        			ic.put(Invitecode.USER_ID, newUserId);
        			ic.put(Invitecode.USE_TIME, System.currentTimeMillis());
        			final String icId = ic.optString(Keys.OBJECT_ID);

        			invitecodeMgmtService.updateInvitecode(icId, ic);
        		}
        		context.renderJSONValue(Verifycode.USER_ID, newUserId);
        		        		
                context.renderTrueResult().renderMsg(langPropsService.get("verifycodeSentLabel"));
        	} else {
        		context.renderMsg(langPropsService.get("invalidVCodeLabel"));
        	}
        } catch (final ServiceException e) {
            final String msg = langPropsService.get("registerFailLabel") + " - " + e.getMessage();
            LOGGER.log(Level.ERROR, msg + "[name={0}, mobile={1}]", name, mobile);

            context.renderMsg(msg);
        }
    }

    /**
     * Register Step 2.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/register2", method = HttpMethod.POST)
    @Before(UserRegister2Validation.class)
    public void register2(final RequestContext context) {
        context.renderJSON();

        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();
        final JSONObject requestJSONObject = (JSONObject) context.attr(Keys.REQUEST);

        final String password = requestJSONObject.optString(User.USER_PASSWORD); // Hashed
        //final int appRole = requestJSONObject.optInt(UserExt.USER_APP_ROLE);
        final String referral = requestJSONObject.optString(Common.REFERRAL);
        final String userId = requestJSONObject.optString(UserExt.USER_T_ID);
        final String jobCode = requestJSONObject.optString(UserJob.JOB_CODE);

        String name = null;
        //String email = null;
        String mobile = null;
        try {
            final JSONObject user = userQueryService.getUser(userId);
            if (null == user || UserExt.USER_STATUS_C_VALID == user.optInt(UserExt.USER_STATUS)) {
                context.renderMsg(langPropsService.get("registerFailLabel") + " - " + "User Not Found");

                return;
            }

            name = user.optString(User.USER_NAME);
            //email = user.optString(User.USER_EMAIL);
            mobile = user.optString(UserExt.USER_MOBOLE);

            //modify by daodao 默认一个角色先
            //user.put(UserExt.USER_APP_ROLE, appRole);
            user.put(UserExt.USER_APP_ROLE, UserExt.USER_APP_ROLE_C_PAINTER);
            user.put(User.USER_PASSWORD, password);
            user.put(UserExt.USER_STATUS, UserExt.USER_STATUS_C_VALID);

            userMgmtService.addUser(user);
            //add by daodao add job info at 2019/2/14
            JSONObject userJob = new JSONObject();
            userJob.put(UserExt.USER_T_ID, userId);
            userJob.put(UserJob.JOB_CODE,jobCode);
            userJobMgmtService.addUserJobLeave(userJob);
            //add over

            Sessions.login(response, userId, false);

            final String ip = Requests.getRemoteAddr(request);
            userMgmtService.updateOnlineStatus(user.optString(Keys.OBJECT_ID), ip, true, true);

            if (StringUtils.isNotBlank(referral) && !UserRegisterValidation.invalidUserName(referral)) {
                final JSONObject referralUser = userQueryService.getUserByName(referral);
                if (null != referralUser) {
                    final String referralId = referralUser.optString(Keys.OBJECT_ID);
                    // Point
                    pointtransferMgmtService.transfer(Pointtransfer.ID_C_SYS, userId,
                            Pointtransfer.TRANSFER_TYPE_C_INVITED_REGISTER,
                            Pointtransfer.TRANSFER_SUM_C_BE_INVITE_REGISTER, referralId, System.currentTimeMillis(), "");
                    pointtransferMgmtService.transfer(Pointtransfer.ID_C_SYS, referralId,
                            Pointtransfer.TRANSFER_TYPE_C_INVITE_REGISTER,
                            Pointtransfer.TRANSFER_SUM_C_INVITE_REGISTER, userId, System.currentTimeMillis(), "");

                    final JSONObject notification = new JSONObject();
                    notification.put(Notification.NOTIFICATION_USER_ID, referralId);
                    notification.put(Notification.NOTIFICATION_DATA_ID, userId);

                    notificationMgmtService.addInvitationLinkUsedNotification(notification);
                }
            }

            final JSONObject ic = invitecodeQueryService.getInvitecodeByUserId(userId);
            if (null != ic && Invitecode.STATUS_C_UNUSED == ic.optInt(Invitecode.STATUS)) {
                ic.put(Invitecode.STATUS, Invitecode.STATUS_C_USED);
                ic.put(Invitecode.USER_ID, userId);
                ic.put(Invitecode.USE_TIME, System.currentTimeMillis());
                final String icId = ic.optString(Keys.OBJECT_ID);

                invitecodeMgmtService.updateInvitecode(icId, ic);

                final String icGeneratorId = ic.optString(Invitecode.GENERATOR_ID);
                if (StringUtils.isNotBlank(icGeneratorId) && !Pointtransfer.ID_C_SYS.equals(icGeneratorId)) {
                    pointtransferMgmtService.transfer(Pointtransfer.ID_C_SYS, icGeneratorId,
                            Pointtransfer.TRANSFER_TYPE_C_INVITECODE_USED,
                            Pointtransfer.TRANSFER_SUM_C_INVITECODE_USED, userId, System.currentTimeMillis(), "");

                    final JSONObject notification = new JSONObject();
                    notification.put(Notification.NOTIFICATION_USER_ID, icGeneratorId);
                    notification.put(Notification.NOTIFICATION_DATA_ID, userId);

                    notificationMgmtService.addInvitecodeUsedNotification(notification);
                }
            }

            context.renderTrueResult();

            LOGGER.log(Level.INFO, "Registered a user [name={0}, mobile={1}]", name, mobile);
        } catch (final ServiceException e) {
            final String msg = langPropsService.get("registerFailLabel") + " - " + e.getMessage();
            LOGGER.log(Level.ERROR, msg + " [name={0}, mobile={1}]", name, mobile);

            context.renderMsg(msg);
        }
    }

    /**
     * Logins user.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/login", method = HttpMethod.POST)
    public void login(final RequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        context.renderJSON().renderMsg(langPropsService.get("loginFailLabel"));

        JSONObject requestJSONObject;
        try {
            requestJSONObject = context.requestJSON();
        } catch (final Exception e) {
            context.renderMsg(langPropsService.get("paramsParseFailedLabel"));

            return;
        }

        final String nameOrMobile = requestJSONObject.optString("nameOrMobile");

        try {
            JSONObject user = userQueryService.getUserByName(nameOrMobile);
            if (null == user) {
            	//remove by daodao at 20190130
                //user = userQueryService.getUserByEmail(nameOrEmail);
            	user = userQueryService.getUserByMobile(nameOrMobile);
            }

            if (null == user) {
                context.renderMsg(langPropsService.get("notFoundUserLabel"));

                return;
            }

            if (UserExt.USER_STATUS_C_INVALID == user.optInt(UserExt.USER_STATUS)) {
                userMgmtService.updateOnlineStatus(user.optString(Keys.OBJECT_ID), "", false, true);
                context.renderMsg(langPropsService.get("userBlockLabel"));

                return;
            }

            if (UserExt.USER_STATUS_C_NOT_VERIFIED == user.optInt(UserExt.USER_STATUS)) {
                userMgmtService.updateOnlineStatus(user.optString(Keys.OBJECT_ID), "", false, true);
                context.renderMsg(langPropsService.get("notVerifiedLabel"));

                return;
            }

            if (UserExt.USER_STATUS_C_INVALID_LOGIN == user.optInt(UserExt.USER_STATUS)
                    || UserExt.USER_STATUS_C_DEACTIVATED == user.optInt(UserExt.USER_STATUS)) {
                userMgmtService.updateOnlineStatus(user.optString(Keys.OBJECT_ID), "", false, true);
                context.renderMsg(langPropsService.get("invalidLoginLabel"));

                return;
            }

            final String userId = user.optString(Keys.OBJECT_ID);
            JSONObject wrong = WRONG_PWD_TRIES.get(userId);
            if (null == wrong) {
                wrong = new JSONObject();
            }

            final int wrongCount = wrong.optInt(Common.WRON_COUNT);
            if (wrongCount > 3) {
                final String captcha = requestJSONObject.optString(CaptchaProcessor.CAPTCHA);
                if (!StringUtils.equals(wrong.optString(CaptchaProcessor.CAPTCHA), captcha)) {
                    context.renderMsg(langPropsService.get("captchaErrorLabel"));
                    context.renderJSONValue(Common.NEED_CAPTCHA, userId);

                    return;
                }
            }

            final String userPassword = user.optString(User.USER_PASSWORD);
            if (userPassword.equals(requestJSONObject.optString(User.USER_PASSWORD))) {
                final String token = Sessions.login(response, userId, requestJSONObject.optBoolean(Common.REMEMBER_LOGIN));

                final String ip = Requests.getRemoteAddr(request);
                userMgmtService.updateOnlineStatus(user.optString(Keys.OBJECT_ID), ip, true, true);

                context.renderMsg("").renderTrueResult();
                context.renderJSONValue(Keys.TOKEN, token);

                WRONG_PWD_TRIES.remove(userId);

                return;
            }

            if (wrongCount > 2) {
                context.renderJSONValue(Common.NEED_CAPTCHA, userId);
            }

            wrong.put(Common.WRON_COUNT, wrongCount + 1);
            WRONG_PWD_TRIES.put(userId, wrong);

            context.renderMsg(langPropsService.get("wrongPwdLabel"));
        } catch (final ServiceException e) {
            context.renderMsg(langPropsService.get("loginFailLabel"));
        }
    }

    /**
     * Logout.
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/logout", method = HttpMethod.GET)
    public void logout(final RequestContext context) {
        final HttpServletRequest request = context.getRequest();

        final JSONObject user = Sessions.getUser();
        if (null != user) {
            Sessions.logout(user.optString(Keys.OBJECT_ID), context.getResponse());
        }

        String destinationURL = context.param(Common.GOTO);
        if (StringUtils.isBlank(destinationURL)) {
            destinationURL = context.header("referer");
        }

        context.sendRedirect(destinationURL);
    }
}
