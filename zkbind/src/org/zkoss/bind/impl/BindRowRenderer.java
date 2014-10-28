/* BindRowRenderer.java

	Purpose:
		
	Description:
		
	History:
		Aug 16, 2011 10:34:50 AM, Created by henrichen

Copyright (C) 2011 Potix Corporation. All Rights Reserved.
*/

package org.zkoss.bind.impl;

import org.zkoss.bind.sys.TemplateResolver;
import org.zkoss.lang.Objects;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.util.ForEachStatus;
import org.zkoss.zk.ui.util.Template;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupfoot;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;

/**
 * Row renderer for binding.
 * @author henrichen
 * @since 6.0.0
 */
public class BindRowRenderer extends AbstractRenderer implements RowRenderer<Object> {
	private static final long serialVersionUID = 1463169907348730644L;
	public void render(final Row row, final Object data, final int index) {
		final Rows rows = (Rows)row.getParent();
		final Grid grid = (Grid)rows.getParent();
		final int size = grid.getModel().getSize();
		final String tmn = row instanceof Group?"model:group":row instanceof Groupfoot?"model:groupfoot":"model";
		final Template tm = resolveTemplate(grid,row,data,index,size,tmn);
		if (tm == null) {
			final Label label = newRenderLabel(Objects.toString(data));
			label.applyProperties();
			label.setParent(row);
			row.setValue(data);
		} else {
			final ForEachStatus iterStatus = new AbstractForEachStatus(){//provide iteration status in this context
				private static final long serialVersionUID = 1L;
				
				public int getIndex() {
					return index;
				}
				
				public Object getCurrent(){
					return data;
				}
				
				public Integer getEnd(){
					return size;
				}
			};
			
			final String var = (String) tm.getParameters().get(EACH_ATTR);
			final String varnm = var == null ? EACH_VAR : var; //var is not specified, default to "each"
			final String itervar = (String) tm.getParameters().get(STATUS_ATTR);
			final String itervarnm = itervar == null ? ( var==null?EACH_STATUS_VAR:varnm+STATUS_POST_VAR) : itervar; //provide default value if not specified
			
			//bug 1188, EL when nested var and itervar
			Object oldVar = grid.getAttribute(varnm);
			Object oldIter = grid.getAttribute(itervarnm);
			grid.setAttribute(varnm, data);
			grid.setAttribute(itervarnm, iterStatus);
			
			final Component[] items = tm.create(rows, row, null, null);
			
			grid.setAttribute(varnm, oldVar);
			grid.setAttribute(itervarnm, oldIter);
			
			if (items.length != 1)
				throw new UiException("The model template must have exactly one row, not "+items.length);

			final Row nr = (Row)items[0];
			nr.setAttribute(BinderImpl.VAR, varnm);
			addItemReference(grid, nr, index, varnm); //kept the reference to the data, before ON_BIND_INIT
			
			nr.setAttribute(itervarnm, iterStatus);

			//sync open state
			if (nr instanceof Group && row instanceof Group) {
				((Group)nr).setOpen(((Group)row).isOpen());
			}
			
			//ZK-1787 When the viewModel tell binder to reload a list, the other component that bind a bean in the list will reload again
			//move TEMPLATE_OBJECT (was set in resoloveTemplate) to current for check in addTemplateTracking
			nr.setAttribute(TemplateResolver.TEMPLATE_OBJECT, row.removeAttribute(TemplateResolver.TEMPLATE_OBJECT));
			//add template dependency
			addTemplateTracking(grid, nr, data, index, size);
			
			if (nr.getValue() == null) //template might set it
				nr.setValue(data);
			row.setAttribute("org.zkoss.zul.model.renderAs", nr);
				//indicate a new row is created to replace the existent one
			row.detach();
		}
	}
	
	/** Returns the label for the cell generated by the default renderer.
	 */
	private static Label newRenderLabel(String value) {
		final Label label =
			new Label(value != null && value.length() > 0 ? value: " ");
		label.setPre(true); //to make sure &nbsp; is generated, and then occupies some space
		return label;
	}
}
