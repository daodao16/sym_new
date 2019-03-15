package org.b3log.symphony.repository;

import java.util.List;

import org.b3log.latke.repository.AbstractRepository;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.symphony.model.Datadic;
import org.json.JSONObject;

/**
 * datadic repository
 * 
 * @author daodao
 *
 */
@Repository
public class DatadicRepository extends AbstractRepository {

	public DatadicRepository() {
		super(Datadic.DATADIC);
	}

	/**
	 * get data dic by group name
	 * 
	 * @param groupName
	 * @return
	 */
	public List<JSONObject> getDatedicByGroupName(String groupName)
			throws RepositoryException {
		return this.getDatedicByGroupName(groupName, true);
	}

	/**
	 * get data dic by group name order or by parent code asc
	 * 
	 * @param groupName
	 * @param isNeedParentSort
	 * @return
	 * @throws RepositoryException
	 */
	public List<JSONObject> getDatedicByGroupName(String groupName,
			boolean isNeedParentSort) throws RepositoryException {
		Query query = new Query()
				.setFilter(
						new PropertyFilter(Datadic.DATADIC_GROUP_NAME,
								FilterOperator.EQUAL, groupName))
				.setPageCount(1)
				.addSort(Datadic.DATADIC_ORDING, SortDirection.ASCENDING);
		if (isNeedParentSort) {
			query = query.addSort(Datadic.DATADIC_PARENT_CODE,
					SortDirection.ASCENDING);
		}
		List<JSONObject> ret = getList(query);
		return ret;
	}

}
