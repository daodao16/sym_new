package org.b3log.symphony.cache;

import org.b3log.latke.cache.Cache;
import org.b3log.latke.cache.CacheFactory;
import org.b3log.latke.ioc.Singleton;
import org.b3log.symphony.model.JobSkill;
import org.b3log.symphony.util.JSONs;
import org.json.JSONObject;

/**
 * skill cache
 * 
 * @author daodao
 *
 */
@Singleton
public class SkillCache {
	/**
	 * skill cache.
	 */
	private static final Cache CACHE = CacheFactory
			.getCache(JobSkill.JOB_SKILLS);

	static {
		CACHE.setMaxCount(1024);
	}

	/**
	 * Gets an option by the specified skill name.
	 *
	 * @param name
	 *            the specified skill name
	 * @return skill, returns {@code null} if not found
	 */
	public JSONObject getSkillByName(final String name) {
		final JSONObject skill = CACHE.get(name);
		if (null == skill) {
			return null;
		}

		return JSONs.clone(skill);
	}

	/**
	 * Adds or updates the specified skill.
	 *
	 * @param skill
	 *            the specified skill
	 */
	public void putSkill(final JSONObject skill) {
		CACHE.put(skill.optString(JobSkill.SKILL_NAME), JSONs.clone(skill));
	}

	/**
	 * Removes an skill by the specified skill name.
	 *
	 * @param name
	 *            the specified skill name
	 */
	public void removeSkill(final String name) {
		CACHE.remove(name);
	}
}
