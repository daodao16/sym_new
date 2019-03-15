package org.b3log.symphony.repository;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.repository.AbstractRepository;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.symphony.cache.SkillCache;
import org.b3log.symphony.model.JobSkill;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * skill respository
 * 
 * @author daodao
 *
 */
@Repository
public class JobSkillRepository extends AbstractRepository {
	/**
	 * skill cache.
	 */
	@Inject
	private SkillCache skillCache;

	public JobSkillRepository() {
		super(JobSkill.JOB_SKILL);
	}

	/**
	 * add skill for once
	 * 
	 * @param skill
	 * @return
	 * @throws RepositoryException
	 */
	public String addSkillOnce(final JSONObject skill)
			throws RepositoryException {
		final String ret = super.add(skill);

		// 加去重操作，如果多条就要删除多余的，返回唯一的一条，这个操作有点骚
		JSONArray array = this.getSkillsByName(skill
				.optString(JobSkill.SKILL_NAME));
		// 这是个防守操作，高并发的时候可能发生。
		if (array.length() > 1) {
			// 删除多余的，顺序第一条保留
			JSONObject tempSkill;
			for (int i = 1; i < array.length(); i++) {
				tempSkill = array.optJSONObject(i);
				if (!StringUtils.equals(skill.optString(Keys.OBJECT_ID),
						tempSkill.optString(Keys.OBJECT_ID))) {
					super.remove(tempSkill.optString(Keys.OBJECT_ID));
				}
			}
			return array.optJSONObject(0).getString(Keys.OBJECT_ID);
		}
		// add to cache
		skillCache.putSkill(skill);

		return ret;
	}

	/**
	 * get skill by name
	 * 
	 * @param name
	 * @return
	 * @throws RepositoryException
	 */
	public JSONObject getSkillByName(final String name)
			throws RepositoryException {
		JSONObject ret = skillCache.getSkillByName(name);
		if (null != ret) {
			return ret;
		}

		// get skill by name in DB
		JSONArray array = this.getSkillsByName(name);

		if (array == null || array.length() == 0) {
			return null;
		}

		ret = array.optJSONObject(0);

		skillCache.putSkill(ret);

		return ret;
	}

	/**
	 * get skills by name
	 * 
	 * @param name
	 * @return
	 * @throws RepositoryException
	 */
	private JSONArray getSkillsByName(final String name)
			throws RepositoryException {
		final Query query = new Query()
				.setFilter(
						new PropertyFilter(JobSkill.SKILL_NAME,
								FilterOperator.EQUAL, name))
				.addSort(JobSkill.SKILL_CREATETIME, SortDirection.ASCENDING)
				.setPageCount(1);

		JSONObject result = get(query);
		JSONArray array = result.optJSONArray(Keys.RESULTS);

		return array;
	}

}
