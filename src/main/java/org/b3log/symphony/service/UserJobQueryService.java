package org.b3log.symphony.service;

import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.CompositeFilterOperator;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.service.annotation.Service;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.model.UserJob;
import org.b3log.symphony.repository.UserJobRepository;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * job query service
 * 
 * @author daodao
 *
 */
@Service
public class UserJobQueryService {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(UserJobQueryService.class);

	@Inject
	private UserJobRepository userJobRepository;

	public JSONObject getLatestUserJobByType(String userId, String jobType) {
		JSONObject userJob = null;
		Query query = new Query()
				.setFilter(
						CompositeFilterOperator.and(
								new PropertyFilter(UserExt.USER_T_ID,
										FilterOperator.EQUAL, userId),
								new PropertyFilter(UserJob.JOB_TYPE,
										FilterOperator.EQUAL, jobType)))
				.setPageCount(1)
				.addSort(UserJob.CREATE_TIME, SortDirection.DESCENDING);
		try {
			JSONObject result = userJobRepository.get(query);
			JSONArray array = result.optJSONArray(Keys.RESULTS);
			if (array.length() > 0) {
				userJob = array.optJSONObject(0);
			}
		} catch (final RepositoryException e) {
			LOGGER.log(Level.ERROR, "Loads user job info error", e);
		}

		return userJob;
	}

}
