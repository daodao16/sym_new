package org.b3log.symphony.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.Locales;
import org.b3log.symphony.model.Datadic;
import org.b3log.symphony.repository.DatadicRepository;
import org.json.JSONObject;

/**
 * job info query service
 * 
 * @author daodao
 *
 */
@Service
public class JobQueryService {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(JobQueryService.class);

	/**
	 * 最大返回行数
	 */
	private static final int MAX_RESULT = 20;

	/**
	 * DatadicRepository repository.
	 */
	@Inject
	private DatadicRepository datadicRepository;

	/**
	 * Language service.
	 */
	@Inject
	private LangPropsService langPropsService;

	/**
	 * all job info
	 */
	private List<JSONObject> allJobs = new ArrayList<JSONObject>();

	/**
	 * all leave period
	 */
	private List<JSONObject> leavePeriods = null;

	/**
	 * all leave reasons
	 */
	private List<JSONObject> leaveReasons = null;

	/**
	 * all job cat
	 */
	private static final Map<String, JSONObject> jobCategoryMap = new HashMap<String, JSONObject>();

	/**
	 * get jobs by key words
	 * 
	 * @param keyWords
	 * @return
	 */
	public List<JSONObject> getJobs(String keyWords) {
		List<JSONObject> result = new ArrayList<JSONObject>();

		if (allJobs.size() > 0) {
			int total = 0;
			for (JSONObject job : allJobs) {
				// 校验是否包含关键词
				String jobName = job.optString(Datadic.DATADIC_VALUE);
				if (StringUtils.containsIgnoreCase(jobName, keyWords)) {
					job.put(Datadic.JOB_CAT_NAME,
							jobCategoryMap.get(
									job.optString(Datadic.DATADIC_PARENT_CODE))
									.optString(Datadic.DATADIC_VALUE));
					result.add(job);
					total++;
				}
				// 已经到最大返回数了，可以直接推出了，不需要再继续了
				if (total >= MAX_RESULT) {
					break;
				}
			}
		}

		return result;
	}

	/**
	 * get all leave periods data
	 * 
	 * @return
	 */
	public List<JSONObject> getAllJobLeavePeriods() {
		return this.leavePeriods;
	}

	/**
	 * get all leave reasons data
	 * 
	 * @return
	 */
	public List<JSONObject> getAllJobLeaveReasons() {
		return this.leaveReasons;
	}

	/**
	 * init all jobs
	 */
	public void initJobs() {
		// init category
		initJobCategory();
		LOGGER.log(Level.INFO, "job category init finished");

		// init job infos
		try {
			allJobs = datadicRepository
					.getDatedicByGroupName(Datadic.GROUP_JOB);
			LOGGER.log(Level.INFO, "jobs init finished");

			// 离职周期
			leavePeriods = datadicRepository
					.getDatedicByGroupName(Datadic.GROUP_LEAVE_PERIOD);
			LOGGER.log(Level.INFO, "jobs leave period init finished");

			// 离职原因
			leaveReasons = datadicRepository.getDatedicByGroupName(
					Datadic.GROUP_LEAVE_REASON, false);
			LOGGER.log(Level.INFO, "jobs leave resons init finished");

		} catch (final RepositoryException e) {
			LOGGER.log(Level.ERROR, "init job failed", e);
		}
	}

	/**
	 * init all job category
	 */
	private void initJobCategory() {
		JSONObject category = null;
		final Map<String, String> labels = langPropsService.getAll(Locales
				.getLocale());
		for (final Map.Entry<String, String> entry : labels.entrySet()) {
			final String key = entry.getKey();
			if (key.startsWith("jobCat")) {
				category = new JSONObject();
				category.put(Datadic.DATADIC_CODE, key);
				category.put(Datadic.DATADIC_VALUE, entry.getValue());
				jobCategoryMap.put(key, category);
			}
		}
	}
}
