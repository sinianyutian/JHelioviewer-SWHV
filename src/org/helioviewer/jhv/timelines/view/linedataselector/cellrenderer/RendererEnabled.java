package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class RendererEnabled extends JHVTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public RendererEnabled() {
        setHorizontalAlignment(CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "small");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(cellBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineRenderable) {
            checkBox.setSelected(((TimelineRenderable) value).isEnabled());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return checkBox;
    }

}
