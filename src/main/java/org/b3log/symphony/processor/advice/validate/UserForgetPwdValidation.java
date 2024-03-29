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

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.latke.servlet.advice.ProcessAdvice;
import org.b3log.latke.servlet.advice.RequestProcessAdviceException;
import org.b3log.symphony.model.UserExt;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * User forget password form validation.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.1.0, Mar 10, 2016
 * @since 1.4.0
 */
@Singleton
public class UserForgetPwdValidation extends ProcessAdvice {
	
	/**
     * mobile regex
     */
    private static final String MOBILE_REGEX  = "(((1[3456789]{1}[0-9]{1}))+\\d{8})";

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    @Override
    public void doAdvice(final RequestContext context) throws RequestProcessAdviceException {
        final HttpServletRequest request = context.getRequest();

        JSONObject requestJSONObject;
        try {
            requestJSONObject = context.requestJSON();
            request.setAttribute(Keys.REQUEST, requestJSONObject);
        } catch (final Exception e) {
            throw new RequestProcessAdviceException(new JSONObject().put(Keys.MSG, e.getMessage()));
        }

        //final String email = requestJSONObject.optString(User.USER_EMAIL);
        final String mobile = requestJSONObject.optString(UserExt.USER_MOBOLE);
        checkField(invalidMobile(mobile), "submitFailedLabel", "invalidUserMobileLabel");
        //final String captcha = requestJSONObject.optString(CaptchaProcessor.CAPTCHA);

        //checkField(CaptchaProcessor.invalidCaptcha(captcha), "submitFailedLabel", "captchaErrorLabel");
        //checkField(!Strings.isEmail(email), "submitFailedLabel", "invalidEmailLabel");
    }
    
    /**
	 * 手机格式校验
	 * @param mobile
	 * @return
	 */
	private boolean invalidMobile(String mobile) {
		if (StringUtils.isBlank(mobile)) {
            return true;
        }
		Pattern p = Pattern.compile(MOBILE_REGEX);
		return !p.matcher(mobile).matches();
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
