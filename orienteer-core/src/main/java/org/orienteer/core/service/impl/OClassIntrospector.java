package org.orienteer.core.service.impl;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.orientechnologies.common.collection.OCollection;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.OSecurityShared;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.apache.wicket.Application;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.orienteer.core.CustomAttributes;
import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.OrienteerWebSession;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.component.table.CheckBoxColumn;
import org.orienteer.core.component.table.ODocumentClassColumn;
import org.orienteer.core.component.table.OEntityColumn;
import org.orienteer.core.component.table.OPropertyValueColumn;
import org.orienteer.core.component.table.OUnknownEntityColumn;
import org.orienteer.core.component.visualizer.IVisualizer;
import org.orienteer.core.component.visualizer.UIVisualizersRegistry;
import org.orienteer.core.module.OrienteerLocalizationModule;
import org.orienteer.core.service.IOClassIntrospector;
import org.orienteer.core.util.CommonUtils;

import ru.ydn.wicket.wicketorientdb.model.ODocumentLinksDataProvider;
import ru.ydn.wicket.wicketorientdb.model.OQueryDataProvider;
import ru.ydn.wicket.wicketorientdb.proto.OClassPrototyper;
import ru.ydn.wicket.wicketorientdb.proto.OPropertyPrototyper;
import ru.ydn.wicket.wicketorientdb.utils.ODocumentORIDConverter;

import java.util.*;

/**
 * Implementation of {@link IOClassIntrospector}
 */
public class OClassIntrospector implements IOClassIntrospector
{

	/**
	 * {@link Predicate} that checks displayable of an specified {@link OProperty}
	 */
	public static class PropertyDisplayablePredicate implements Predicate<OProperty>
	{
		public static final PropertyDisplayablePredicate INSTANCE = new PropertyDisplayablePredicate();
		@Override
		public boolean apply(OProperty input) {
			Boolean value = CustomAttributes.DISPLAYABLE.getValue(input);
			return value!=null?value:false;
		}
	}
	
	/**
	 * {@link Function} to take an order of an specified {@link OProperty}
	 */
	public static class GetOrderOfPropertyFunction implements Function<OProperty, Integer>
	{
		public static final GetOrderOfPropertyFunction INSTANCE = new GetOrderOfPropertyFunction();
		@Override
		public Integer apply(OProperty input) {
			return CustomAttributes.ORDER.getValue(input);
		}
	}
	
	public static final Ordering<OProperty> ORDER_PROPERTIES_BY_ORDER = Ordering.<Integer>natural().nullsLast().onResultOf(GetOrderOfPropertyFunction.INSTANCE);

	
	@Override
	public List<OProperty> getDisplayableProperties(OClass oClass) {
		Collection<OProperty> properties =  oClass.properties();
		Collection<OProperty> filteredProperties = Collections2.filter(properties, PropertyDisplayablePredicate.INSTANCE);
		if(filteredProperties==null || filteredProperties.isEmpty()) filteredProperties = properties;
		return ORDER_PROPERTIES_BY_ORDER.sortedCopy(filteredProperties);
	}

	@Override
	public List<IColumn<ODocument, String>> getColumnsFor(OClass oClass, boolean withCheckbox, IModel<DisplayMode> modeModel) {
		List<IColumn<ODocument, String>> columns = new ArrayList<IColumn<ODocument,String>>();
		if(oClass!=null) {
			List<OProperty> properties = getDisplayableProperties(oClass);
			if(withCheckbox) columns.add(new CheckBoxColumn<ODocument, ORID, String>(ODocumentORIDConverter.INSTANCE));
			OProperty nameProperty = getNameProperty(oClass);
			OEntityColumn entityColumn = new OEntityColumn(nameProperty, true, modeModel);
			columns.add(entityColumn);
			if (!oClass.getSubclasses().isEmpty()) {
				columns.add(new ODocumentClassColumn<String>());
			}
			for (OProperty oProperty : properties)
			{
				if(nameProperty==null || !nameProperty.equals(oProperty))
				{
					Class<?> javaType = oProperty.getType().getDefaultJavaType();
					if(javaType!=null && Comparable.class.isAssignableFrom(javaType))
					{
						columns.add(new OPropertyValueColumn(oProperty.getName(), oProperty, modeModel));
					}
					else
					{
						columns.add(new OPropertyValueColumn(oProperty, modeModel));
					}
				}
			}
		} else {
			columns.add(new OUnknownEntityColumn(new ResourceModel("document.name")));
			columns.add(new ODocumentClassColumn<String>());
		}
		return columns;
	}

	@Override
	public List<ODocument> getNavigationPath(ODocument doc, boolean fromUpToDown) {
		List<ODocument> path = new ArrayList<ODocument>();
		ODocument current = doc;
		boolean cycle;
		while(current!=null)
		{
			cycle = path.contains(current);
			path.add(current);
			if(cycle) break;
			current = getParent(current);
		}
		return fromUpToDown?Lists.reverse(path):path;
	}

	@Override
	public ODocument getParent(ODocument doc) {
		if(doc==null || doc.getSchemaClass()==null) return null;
		OClass oClass = doc.getSchemaClass();
		OProperty parent = CustomAttributes.PROP_PARENT.getHierarchicalValue(oClass);
		if(parent!=null) return doc.field(parent.getName());
		else return null;
	}

	@Override
	public List<String> listTabs(OClass oClass) {
		Set<String> tabs = new HashSet<String>();
		for(OProperty property: oClass.properties())
		{
			String tab = CustomAttributes.TAB.getValue(property);
			if(tab==null) tab = DEFAULT_TAB;
			tabs.add(tab);
		}
		return new ArrayList<String>(tabs);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<OProperty> listProperties(OClass oClass, String tab, final Boolean extended) {
		final String safeTab = tab!=null?tab:DEFAULT_TAB;
		final UIVisualizersRegistry registry = OrienteerWebApplication.get().getUIVisualizersRegistry();
		
		return listProperties(oClass, new Predicate<OProperty>() {

			@Override
			public boolean apply(OProperty input) {
				boolean ret = safeTab.equals(CustomAttributes.TAB.getValue(input, DEFAULT_TAB));
				ret = ret && !CustomAttributes.HIDDEN.getValue(input, false);
				if(!ret || extended==null) return ret;
				else {
					String component = CustomAttributes.VISUALIZATION_TYPE.getValue(input);
					if(component==null) return !extended;
					IVisualizer visualizer = registry.getComponentFactory(input.getType(), component);
					return (visualizer!=null?visualizer.isExtended():false) == extended;
				}
			}
		});
	}
	
	@Override
	public List<OProperty> listProperties(OClass oClass,
			Predicate<OProperty>... predicates) {
		if(oClass==null) return Collections.EMPTY_LIST;
		Collection<OProperty> properties =  oClass.properties();
		Predicate<OProperty> predicate = predicates==null || predicates.length==0?
												 null
												:(predicates.length==1?
															predicates[0]
															:Predicates.and(predicates));
		Collection<OProperty> filteredProperties = predicate!=null?Collections2.filter(properties, predicate):properties;
		return ORDER_PROPERTIES_BY_ORDER.sortedCopy(filteredProperties);
		
	}

	@Override
	public ISortableDataProvider<ODocument, String> prepareDataProviderForProperty(
			OProperty property, IModel<ODocument> documentModel) {
		if(CustomAttributes.CALCULABLE.getValue(property, false))
		{
			String sql = CustomAttributes.CALC_SCRIPT.getValue(property);
			sql = sql.replace("?", ":doc");
			OQueryDataProvider<ODocument> provider = new OQueryDataProvider<ODocument>(sql);
			provider.setParameter("doc", documentModel);
			return provider;
		}
		else
		{
			return new ODocumentLinksDataProvider(documentModel, property);
		}
	}

	@Override
	public OProperty getNameProperty(OClass oClass) {
		if(oClass==null) return null;
		OProperty ret = CustomAttributes.PROP_NAME.getHierarchicalValue(oClass);
		if(ret!=null) return ret;
		ret = oClass.getProperty("name");
		if(ret!=null) return ret;
		for(OProperty p: oClass.properties())
		{
			if(!p.getType().isMultiValue())
			{
				ret = p;
				if(OType.STRING.equals(p.getType())) break;
			}
		}
		return ret;
	}

	@Override
	public String getDocumentName(ODocument doc) {
		if(doc==null) return Application.get().getResourceSettings().getLocalizer().getString("nodoc", null);
		else
		{
			//TODO: Enhance to cover complex naming cases
			OProperty nameProp = getNameProperty(doc.getSchemaClass());
			if(nameProp!=null)
			{
				Object value = doc.field(nameProp.getName());
				if(value==null) return Application.get().getResourceSettings().getLocalizer().getString("noname", null);
				OType type = nameProp.getType();
				Locale locale = OrienteerWebSession.get().getLocale();
				switch (type)
				{
					case DATE:
						return OrienteerWebApplication.DATE_CONVERTER.convertToString((Date)value, locale);
					case DATETIME:
						return OrienteerWebApplication.DATE_TIME_CONVERTER.convertToString((Date)value, locale);
					case LINK:
						return getDocumentName((ODocument)value);
					case EMBEDDEDMAP:
						Map<String, Object> localizations = (Map<String, Object>)value;
						Object localized = CommonUtils.localizeByMap(localizations, true, locale.getLanguage(), Locale.getDefault().getLanguage());
						if(localized!=null) return localized.toString();
					default:
						return value.toString();
				}
			}
			else
			{
				return doc.toString();
			}
		}
	}
	
	@Override
	public OProperty virtualizeField(ODocument doc, String field) {
		OProperty property = OPropertyPrototyper.newPrototype(doc.getClassName());
		property.setName(field);
		OType oType = doc.fieldType(field);
		if(oType==null) oType=OType.ANY;
		property.setType(oType);
		switch (oType) {
			case LINK:
				ODocument link = doc.field(field);
				if(link!=null) property.setLinkedClass(link.getSchemaClass());
				break;
			case LINKBAG:
				OCollection<OIdentifiable> bag = doc.field(field);
				if(bag!=null && bag.size()>0) {
					OIdentifiable linkIdentifiable = bag.iterator().next();
					ORecord record = linkIdentifiable!=null?linkIdentifiable.getRecord():null;
					if(record!=null && record instanceof ODocument) property.setLinkedClass(((ODocument)record).getSchemaClass());
				}
				break;
			case LINKLIST:
			case LINKSET:
				Collection<ODocument> collection = doc.field(field);
				if(collection!=null && !collection.isEmpty()) {
					link = collection.iterator().next();
					if(link!=null) property.setLinkedClass(link.getSchemaClass());
				}
				break;
			case LINKMAP:
				Map<String, ODocument> map = doc.field(field);
				if(map!=null && !map.isEmpty()) {
					link = map.values().iterator().next();
					if(link!=null) property.setLinkedClass(link.getSchemaClass());
				}
				break;
			case EMBEDDED:
				Object value = doc.field(field);
				OType linkedType = OType.getTypeByValue(value);
				if(OType.EMBEDDED.equals(linkedType)) property.setLinkedClass(((ODocument)value).getSchemaClass());
				else property.setLinkedType(linkedType);
				break;
			case EMBEDDEDSET:
			case EMBEDDEDLIST:
				Collection<Object> objectCollection = doc.field(field);
				if(objectCollection!=null && !objectCollection.isEmpty()) {
					value = objectCollection.iterator().next();
					property.setLinkedType(OType.getTypeByValue(value));
				}
				break;
			case EMBEDDEDMAP:
				Map<String, Object> objectMap = doc.field(field);
				if(objectMap!=null && !objectMap.isEmpty()) {
					value = objectMap.values().iterator().next();
					property.setLinkedType(OType.getTypeByValue(value));
				}
				break;
			default:
				break;
		}
		return property;
	}

}
