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
package org.b3log.symphony.processor.advice.validate;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.model.User;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.latke.servlet.advice.ProcessAdvice;
import org.b3log.latke.servlet.advice.RequestProcessAdviceException;
import org.b3log.symphony.model.Option;
import org.b3log.symphony.model.UserJob;
import org.b3log.symphony.service.OptionQueryService;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * UserRegister2Validation for validate {@link org.b3log.symphony.processor.LoginProcessor} register2(Type POST) method.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.1.0, Jul 3, 2016
 * @since 1.3.0
 */
@Singleton
public class UserRegister2Validation extends ProcessAdvice {

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;

    /**
     * Max password length.
     *
     * <p>
     * MD5 32
     * </p>
     */
    private static final int MAX_PWD_LENGTH = 32;

    /**
     * Min password length.
     */
    private static final int MIN_PWD_LENGTH = 1;
    
    /**
     * fix job code length
     */
    private static final int JOB_CODE_LENGTH = 4;

    @Override
    public void doAdvice(final RequestContext context) throws RequestProcessAdviceException {
        final HttpServletRequest request = context.getRequest();

        JSONObject requestJSONObject;
        try {
            requestJSONObject = context.requestJSON();
            request.setAttribute(Keys.REQUEST, requestJSONObject);

            // check if admin allow to register
            final JSONObject option = optionQueryService.getOption(Option.ID_C_MISC_ALLOW_REGISTER);
            if ("1".equals(option.optString(Option.OPTION_VALUE))) {
                throw new Exception(langPropsService.get("notAllowRegisterLabel"));
            }
        } catch (final Exception e) {
            throw new RequestProcessAdviceException(new JSONObject().put(Keys.MSG, e.getMessage()));
        }

        //final int appRole = requestJSONObject.optInt(UserExt.USER_APP_ROLE);
        final String password = requestJSONObject.optString(User.USER_PASSWORD);
        final String jobCode = requestJSONObject.optString(UserJob.JOB_CODE);
        //remove by daodao at 20190202 默认角色了，现在记录的是职位信息
//        checkField(UserExt.USER_APP_ROLE_C_HACKER != appRole
//                && UserExt.USER_APP_ROLE_C_PAINTER != appRole, "registerFailLabel", "invalidAppRoleLabel");
        checkField(invalidUserPassword(password), "registerFailLabel", "invalidPasswordLabel");
        checkField(invalidUserJobCode(jobCode), "registerFailLabel", "invalidJobCodeLabel");
    }

    /**
     * Checks password, length [1, 16].
     *
     * @param password the specific password
     * @return {@code true} if it is invalid, returns {@code false} otherwise
     */
    public static boolean invalidUserPassword(final String password) {
        return password.length() < MIN_PWD_LENGTH || password.length() > MAX_PWD_LENGTH;
    }
    
    /**
     * Checks jobcode,length 4
     * @param jobCode
     * @return
     */
    public static boolean invalidUserJobCode(final String jobCode) {
        if (StringUtils.isBlank(jobCode)) {
            return true;
        }

        final int length = jobCode.length();
        if (length != JOB_CODE_LENGTH) {
            return true;
        }

        return false;
    }

    /**
     * Checks field.
     *
     * @param invalid    the specified invalid flag
     * @param failLabel  the specified fail label
     * @param fieldLabel the specified field label
     * @throws RequestProcessAdviceException request process advice exception
     */
    private void checkField(final boolean invalid, final String failLabel, final String fieldLabel)
            throws RequestProcessAdviceException {
        if (invalid) {
            throw new RequestProcessAdviceException(new JSONObject().put(Keys.MSG, langPropsService.get(failLabel)
                    + " - " + langPropsService.get(fieldLabel)));
        }
    }
}
