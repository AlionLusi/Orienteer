package org.orienteer.core.component.command;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.orienteer.core.component.structuretable.OrienteerStructureTable;
import org.orienteer.core.component.structuretable.StructureTableCommandsToolbar;
import org.orienteer.core.component.table.DataTableCommandsToolbar;
import org.orienteer.core.component.table.OrienteerDataTable;

/**
 * Ajax-enabled {@link Command} which additionally submits a form
 *
 * @param <T>  the type of an entity to which this command can be applied
 */
public class AjaxFormCommand<T> extends AjaxCommand<T>
{
	private static final long serialVersionUID = 1L;

	public AjaxFormCommand(IModel<?> labelModel, OrienteerDataTable<T, ?> table)
	{
		super(labelModel, table);
	}

	public AjaxFormCommand(IModel<?> labelModel,
			OrienteerStructureTable<T, ?> table)
	{
		super(labelModel, table);
	}

	public AjaxFormCommand(String commandId, IModel<?> labelModel)
	{
		super(commandId, labelModel);
	}

	public AjaxFormCommand(String commandId, String labelKey)
	{
		super(commandId, labelKey);
	}

	@Override
	protected AbstractLink newLink(String id) {
		return new AjaxSubmitLink(id)
		{

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				AjaxFormCommand.this.onSubmit(target, form);
				trySendActionPerformed();
			}
			
		};
		/*return new AjaxFallbackLink<Object>(id)
		        {
					@Override
					public void onClick(AjaxRequestTarget target) {
						AjaxCommand.this.onClick(target);
					}
		        };*/
	}
	
	public void onSubmit(AjaxRequestTarget target, Form<?> form) {
		onClick(target);
	}

	@Override
	public void onClick(AjaxRequestTarget target) {

	}

}
