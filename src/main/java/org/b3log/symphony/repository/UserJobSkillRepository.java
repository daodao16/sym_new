package org.b3log.symphony.repository;

import org.b3log.latke.repository.AbstractRepository;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.symphony.model.JobSkill;
import org.b3log.symphony.model.UserJob;

/**
 * user job skill repository
 * 
 * @author daodao
 *
 */
@Repository
public class UserJobSkillRepository extends AbstractRepository {

	public UserJobSkillRepository() {
		super(UserJob.USER_JOB + "_" + JobSkill.JOB_SKILL);
	}

}
