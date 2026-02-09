package com.marginallyclever;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;

public class PanelHelper {
    static public NumberFormatter getNumberFormatterInt() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        //formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(true);
        return formatter;
    }

    /**
     * <p>A convenience method to add a number field to a panel.</p>
     * @param toolTip the tooltip for the field
     * @param value the initial value
     * @return the {@link JFormattedTextField}
     */
    public static JFormattedTextField addNumberFieldInt(String toolTip, int value) {
        return addNumberField(toolTip,value,getNumberFormatterInt());
    }

    /**
     * <p>A convenience method to add a number field to a panel.</p>
     * @param toolTip the tooltip for the field
     * @param value the initial value
     * @param formatter the {@link NumberFormatter} to use
     * @return the {@link JFormattedTextField}
     */
    public static JFormattedTextField addNumberField(String toolTip, double value, NumberFormatter formatter) {
        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setValue(value);
        field.setToolTipText(toolTip);
        field.setColumns(3);
        field.setMinimumSize(new Dimension(0,20));
        return field;
    }
}
