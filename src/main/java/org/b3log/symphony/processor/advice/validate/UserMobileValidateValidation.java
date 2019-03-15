package org.b3log.symphony.processor.advice.validate;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.latke.servlet.advice.ProcessAdvice;
import org.b3log.latke.servlet.advice.RequestProcessAdviceException;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.processor.CaptchaProcessor;
import org.b3log.symphony.service.OptionQueryService;
import org.json.JSONObject;

/**
 * UserMobileValidateValidation for validate {@link org.b3log.symphony.processor.LoginProcessor} register(Type POST) method.
 *
 * @author daodao
 * @version 3.6.4.1, Jan 1, 2019
 * @since 3.6.4
 */
@Singleton
public class UserMobileValidateValidation extends ProcessAdvice {	
    
    /**
     * mobile regex
     */
    private static final String MOBILE_REGEX  = "(((1[3456789]{1}[0-9]{1}))+\\d{8})";
	
	 /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;
    
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
       
        final String captcha = requestJSONObject.optString(CaptchaProcessor.CAPTCHA);
        checkField(CaptchaProcessor.invalidCaptcha(captcha), "registerFailLabel", "captchaErrorLabel");

        final String mobile = requestJSONObject.optString(UserExt.USER_MOBOLE);
        checkField(invalidMobile(mobile), "registerFailLabel", "invalidUserMobileLabel");
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
