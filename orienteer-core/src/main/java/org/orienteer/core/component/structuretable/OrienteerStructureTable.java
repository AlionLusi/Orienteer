package org.orienteer.core.component.structuretable;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.model.IModel;
import org.orienteer.core.component.ICommandsSupportComponent;
import org.orienteer.core.component.command.Command;
import org.orienteer.core.component.meta.AbstractMetaPanel;
import org.orienteer.core.component.meta.IMetaContext;

/**
 * {@link StructureTable} which allow to use meta micro-framework ( {@link IMetaContext} )
 *
 * @param <T> the type of main object for this table
 * @param <C> the type of criterias to be used for this table
 */
public abstract class OrienteerStructureTable<T, C> extends StructureTable<T, C> implements IMetaContext<C>, ICommandsSupportComponent<T>
{
	private static final long serialVersionUID = 1L;
	private StructureTableCommandsToolbar<T> commandsToolbar;

	public OrienteerStructureTable(String id, IModel<T> model, IModel<List<? extends C>> criteriesModel)
	{
		super(id, model, criteriesModel);
		initialize();
	}

	public OrienteerStructureTable(String id, IModel<T> model, List<? extends C> list)
	{
		super(id, model, list);
		initialize();
	}

	protected void initialize()
	{
		commandsToolbar = new StructureTableCommandsToolbar<T>(this);
		addTopToolbar(commandsToolbar);
	}

	public StructureTableCommandsToolbar<T> getCommandsToolbar() {
		return commandsToolbar;
	}
	
	@Override
	public OrienteerStructureTable<T, C> addCommand(Command<T> command)
	{
		getCommandsToolbar().addCommand(command);
		return this;
	}
	
	@Override
	public OrienteerStructureTable<T, C> removeCommand(Command<T> command) {
		getCommandsToolbar().removeCommand(command);
		return this;
	}

	@Override
	public String newCommandId() {
		return getCommandsToolbar().newCommandId();
	}

	@Override
	protected Component getValueComponent(String id, IModel<C> rowModel) {
		// TODO Auto-generated method stub
		return null;
	}

	public OrienteerStructureTable<T, C> setCaptionModel(IModel<String> captionModel)
	{
		super.setCaptionModel(captionModel);
		return this;
	}
	
	@Override
	public OrienteerStructureTable<T, C> getContextComponent() {
		return this;
	}
	
	@Override
	public <K extends AbstractMetaPanel<?, C, ?>> K getMetaComponent(C critery) {
		return AbstractMetaPanel.getMetaComponent(this, critery);
	}
	
	@Override
	public void onEvent(IEvent<?> event) {
		if(event.getPayload() instanceof AjaxRequestTarget && Broadcast.BUBBLE.equals(event.getType()))
		{
			AjaxRequestTarget target = ((AjaxRequestTarget)event.getPayload());
			target.add(this);
			onAjaxUpdate(target);
			event.stop();
		}
	}
	
	public void onAjaxUpdate(AjaxRequestTarget target)
	{
	}

}
