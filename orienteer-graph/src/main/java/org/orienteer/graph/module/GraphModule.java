package org.orienteer.graph.module;

import com.google.inject.Singleton;
import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.module.AbstractOrienteerModule;
import org.orienteer.core.util.OSchemaHelper;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;

/**
 * {@link AbstractOrienteerModule} to provide graph extentions
 */
@Singleton
public class GraphModule extends AbstractOrienteerModule {

	public static final String EDGE_CLASS_NAME = "E";
	public static final String VERTEX_CLASS_NAME = "V";

	protected GraphModule() {
		super("graph", 1);

		OrienteerWebApplication.get().registerWidgets("org.orienteer.graph.component.widget");
	}
	
	@Override
	public void onInstall(OrienteerWebApplication app, ODatabaseDocument db) {
		onUpdate(app, db, 0, getVersion());
	}

	@Override
	public void onUpdate(OrienteerWebApplication app, ODatabaseDocument db,
			int oldVersion, int newVersion) {
		if(oldVersion>=newVersion) return;
		switch (oldVersion+1)
		{
			case 1:
				onUpdateToFirstVesion(app, db);
				break;
			default:
				break;
		}
		if(oldVersion+1<newVersion) onUpdate(app, db, oldVersion + 1, newVersion);
	}

	public void onUpdateToFirstVesion(OrienteerWebApplication app, ODatabaseDocument db)
	{
		OSchemaHelper helper = OSchemaHelper.bind(db);
		helper.oClass(VERTEX_CLASS_NAME)
			  .oClass(EDGE_CLASS_NAME);
	}

}
