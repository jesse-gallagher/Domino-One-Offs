package controller;

import static com.googlecode.cqengine.query.QueryFactory.*;

import index.IndexManager;
import index.IndexEntry;

import java.util.*;

import javax.faces.event.PhaseEvent;

import frostillicus.xsp.controller.BasicXPageController;
import frostillicus.xsp.util.FrameworkUtils;

import com.google.common.collect.*;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;

public class home extends BasicXPageController {
	private static final long serialVersionUID = 1L;

	@Override
	public void afterPageLoad() throws Exception {
		super.afterPageLoad();

		System.out.println(HashMultimap.create());
	}

	@Override
	public void beforeRenderResponse(final PhaseEvent event) throws Exception {
		super.beforeRenderResponse(event);

		IndexManager man = IndexManager.get();
		IndexedCollection<IndexEntry> index = man.getValue("Import View");
		Map<String, Object> requestScope = FrameworkUtils.getRequestScope();
		Query<IndexEntry> query = equal(IndexEntry.attr("firstname"), "Joe");
		ResultSet<IndexEntry> resultSet = index.retrieve(query);

		// For now, just turn the Iterable result into an ArrayList so repeat controls
		// know how to consume it - this would be better with a thin wrapper
		requestScope.put("result", Lists.newArrayList(resultSet));
	}
}
