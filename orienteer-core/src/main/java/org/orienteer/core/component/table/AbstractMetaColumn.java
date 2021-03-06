package org.orienteer.core.component.table;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.orienteer.core.component.meta.AbstractMetaPanel;
import org.orienteer.core.component.meta.OIndexMetaPanel;

/**
 * {@link IColumn} to display meta components ( {@link AbstractMetaPanel} )
 *
 * @param <T> the type of the object that will be rendered in this column's cells
 * @param <C> the type of criteria for this column
 * @param <S> the type of the sort property
 */
public abstract class AbstractMetaColumn<T, C, S> extends AbstractColumn<T, S>
{
	private static final long serialVersionUID = 1L;
	private IModel<C> criteryModel;
	private IModel<String> labelModel;
	private AbstractMetaPanel<T, C, ?> metaPanel;
	
	public AbstractMetaColumn(IModel<C> criteryModel) {
		super(null);
		this.criteryModel = criteryModel;
	}
	
	public AbstractMetaColumn(final S sortProperty, IModel<C> criteryModel) {
		super(null, sortProperty);
		this.criteryModel = criteryModel;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
		cellItem.add(metaPanel = newMetaPanel(componentId, criteryModel, rowModel));
	}
	
	public IModel<C> getCriteryModel() {
		return criteryModel;
	}

	protected abstract <V> AbstractMetaPanel<T, C, V> newMetaPanel(String componentId, IModel<C> criteryModel, IModel<T> rowModel);
	
	protected abstract IModel<String> newLabelModel();
	
	public IModel<String> getLabelModel(){
		if(labelModel==null)
		{
			labelModel = newLabelModel();
		}
		return labelModel;
	}
	
	public AbstractMetaColumn<T, C, S> setLabelModel(IModel<String> labelModel)
	{
		this.labelModel = labelModel;
		return this;
	}
	
	@Override
	public final IModel<String> getDisplayModel() {
		return getLabelModel();
	}

	@Override
	public void detach() {
		super.detach();
		if(criteryModel!=null)criteryModel.detach();
		if(labelModel!=null) labelModel.detach();
	}

	

}