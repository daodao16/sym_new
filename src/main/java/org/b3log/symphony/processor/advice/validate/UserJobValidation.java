package org.b3log.symphony.processor.advice.validate;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.ioc.Singleton;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.latke.servlet.advice.ProcessAdvice;
import org.b3log.latke.servlet.advice.RequestProcessAdviceException;
import org.b3log.symphony.model.JobSkill;
import org.b3log.symphony.model.UserJob;
import org.json.JSONObject;

/**
 * UserJobValidation for validate
 * {@link org.b3log.symphony.processor.LoginProcessor} jobInfoGuideStep(Type
 * POST) method.
 * 
 * @author daodao
 *
 */
@Singleton
public class UserJobValidation extends ProcessAdvice {
	/**
	 * Language service.
	 */
	@Inject
	private LangPropsService langPropsService;

	/**
	 * dic code length.
	 */
	private static final int CODE_LENGTH = 4;

	/**
	 * skill max length
	 */
	private static final int SKILL_MAX_LENGTH = 255;

	/**
	 * skill max num
	 */
	private static final int SKILL_MAX_NUM = 5;

	@Override
	public void doAdvice(final RequestContext context)
			throws RequestProcessAdviceException {
		final HttpServletRequest request = context.getRequest();

		JSONObject requestJSONObject;
		try {
			requestJSONObject = context.requestJSON();
			request.setAttribute(Keys.REQUEST, requestJSONObject);
		} catch (final Exception e) {
			throw new RequestProcessAdviceException(new JSONObject().put(
					Keys.MSG, e.getMessage()));
		}
		
		String leavePeriod = requestJSONObject.optString(UserJob.LEAVE_PERIOD);
		String leaveReason = requestJSONObject.optString(UserJob.LEAVE_REASON);
		
		checkField(invalidJobSelectField(leavePeriod), "userJobFailLabel", "invalidPeriodLabel");
		checkField(invalidJobSelectField(leaveReason), "userJobFailLabel", "invalidReasonLabel");
		
		String skills = requestJSONObject.optString(JobSkill.MAIN_SKILL);
		checkField(invalidJobSkll(skills), "userJobFailLabel", "invalidMainSkillLabel");
	}

	/**
	 * select field check
	 * 
	 * @param name
	 * @return
	 */
	private static boolean invalidJobSelectField(String value) {
		if (StringUtils.isBlank(value)) {
			return true;
		}

		int length = value.length();
		if (length != CODE_LENGTH) {
			return true;
		}

		return false;
	}

	/**
	 * user skill check
	 * 
	 * @param skills
	 * @return
	 */
	private static boolean invalidJobSkll(String skills) {
		if (StringUtils.isBlank(skills)) {
			return false;
		}

		int length = skills.length();
		if (length > SKILL_MAX_LENGTH) {
			return true;
		}

		String[] skillArray = skills.split(" ");
		if (skillArray.length > SKILL_MAX_NUM) {
			return true;
		}

		return false;
	}

	/**
	 * Checks field.
	 *
	 * @param invalid
	 *            the specified invalid flag
	 * @param failLabel
	 *            the specified fail label
	 * @param fieldLabel
	 *            the specified field label
	 * @throws RequestProcessAdviceException
	 *             request process advice exception
	 */
	private void checkField(final boolean invalid, final String failLabel,
			final String fieldLabel) throws RequestProcessAdviceException {
		if (invalid) {
			throw new RequestProcessAdviceException(new JSONObject().put(
					Keys.MSG, langPropsService.get(failLabel) + " - "
							+ langPropsService.get(fieldLabel)));
		}
	}
}
