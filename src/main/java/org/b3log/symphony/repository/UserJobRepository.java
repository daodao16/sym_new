package org.b3log.symphony.repository;

import org.b3log.latke.repository.AbstractRepository;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.symphony.model.UserJob;

/**
 * user job repository
 * 
 * @author daodao
 *
 */
@Repository
public class UserJobRepository extends AbstractRepository {

	public UserJobRepository() {
		super(UserJob.USER_JOB);
	}

}
