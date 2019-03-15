package org.b3log.symphony.service;

import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.annotation.Transactional;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.symphony.model.JobSkill;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.model.UserJob;
import org.b3log.symphony.repository.JobSkillRepository;
import org.b3log.symphony.repository.UserJobRepository;
import org.b3log.symphony.repository.UserJobSkillRepository;
import org.json.JSONObject;

/**
 * job manage service
 * 
 * @author daodao
 *
 */
@Service
public class UserJobMgmtService {
	private static final Logger LOGGER = Logger
			.getLogger(UserJobMgmtService.class);

	@Inject
	private UserJobRepository userJobRepository;

	@Inject
	private JobSkillRepository jobSkillRepository;

	@Inject
	private UserJobSkillRepository userJobSkillRepository;

	/**
	 * add user job info for a new user has leave a company
	 * 
	 * @param userJob
	 * @return
	 */
	@Transactional
	public String addUserJobLeave(final JSONObject userJob)
			throws ServiceException {
		if (userJob == null) {
			return null;
		}
		String jobId;
		userJob.put(UserJob.COMPANY_NAME, "");
		userJob.put(UserJob.JOB_TYPE, UserJob.JOB_TYPE_LEAVE);
		userJob.put(UserJob.LEAVE_TYPE, UserJob.LEAVE_TYPE_NOT_LEAVE);
		userJob.put(UserJob.LEAVE_PERIOD, "");
		userJob.put(UserJob.LEAVE_REASON, "");
		userJob.put(UserJob.CREATE_TIME, System.currentTimeMillis());
		try {
			jobId = userJobRepository.add(userJob);
		} catch (RepositoryException e) {
			LOGGER.log(Level.ERROR, "Adds user job failed", e);

			throw new ServiceException(e.getMessage());
		}

		return jobId;
	}

	/**
	 * add user job info and skill info when in guide page
	 * 
	 * @param userJob
	 * @param skills
	 */
	@Transactional
	public void updateUserJob(JSONObject userJob, String[] skills)
			throws ServiceException {
		try {
			// 修改职位信息
			userJobRepository
					.update(userJob.optString(Keys.OBJECT_ID), userJob);
			// 匹配技能信息,记录技能和用户职位信息关联数据（包含技能去重）
			if (skills != null && skills.length > 0) {
				JSONObject skillObj;
				for (String skill : skills) {
					skillObj = jobSkillRepository.getSkillByName(skill
							.toLowerCase());
					if (skillObj == null) {
						// 增加新的基础技能
						skillObj = addNewSkill(skill);
					}
					// 记录关联数据
					addUserJobSkill(userJob, skillObj);
				}
			}
		} catch (RepositoryException e) {
			LOGGER.log(Level.ERROR, "update user job failed", e);

			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 * add user & skill relation data
	 * 
	 * @param userJob
	 * @param skillObj
	 * @throws RepositoryException
	 */
	private void addUserJobSkill(JSONObject userJob, JSONObject skillObj)
			throws RepositoryException {
		JSONObject userSkillObj = new JSONObject();

		userSkillObj.put(JobSkill.SKILL_T_ID, skillObj.optString(Keys.OBJECT_ID));
		userSkillObj.put(UserExt.USER_T_ID, userJob.optString(UserExt.USER_T_ID));

		userJobSkillRepository.add(userSkillObj);
	}

	/**
	 * add new skill
	 * 
	 * @param skill
	 * @return
	 * @throws RepositoryException
	 */
	private JSONObject addNewSkill(String skill) throws RepositoryException {
		JSONObject skillObj = new JSONObject();
		skillObj.put(JobSkill.SKILL_NAME, skill.toLowerCase());
		skillObj.put(JobSkill.SKILL_DESC, "");
		skillObj.put(JobSkill.SKILL_CREATETIME, System.currentTimeMillis());

		String skillId = jobSkillRepository.addSkillOnce(skillObj);
		skillObj.put(Keys.OBJECT_ID, skillId);
		return skillObj;
	}
}
