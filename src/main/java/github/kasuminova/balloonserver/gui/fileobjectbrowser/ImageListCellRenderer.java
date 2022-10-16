package github.kasuminova.balloonserver.gui.fileobjectbrowser;

import github.kasuminova.balloonserver.utils.ModernColors;
import github.kasuminova.balloonserver.utils.SvgIcons;
import github.kasuminova.messages.fileobject.AbstractLiteFileObject;
import github.kasuminova.messages.fileobject.LiteDirectoryObject;
import github.kasuminova.messages.fileobject.LiteFileObject;

import java.awt.*;
import javax.swing.*;

public class ImageListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof AbstractLiteFileObject abstractFileObject) {
            setText(abstractFileObject.getName());		//设置文字
            if (abstractFileObject instanceof LiteFileObject) {
                setIcon(SvgIcons.FILE_ICON);
            } else if (abstractFileObject instanceof LiteDirectoryObject){
                setIcon(SvgIcons.DIR_ICON);
            }
        } else {
            setText(value.toString());		//设置文字
            setIcon(SvgIcons.FILE_ICON);
        }

        if (isSelected) {		//当某个元素被选中时
            setForeground(Color.WHITE);		//设置前景色（文字颜色）为白色
            setBackground(ModernColors.BLUE);		//设置背景色为蓝色
        } else {		//某个元素未被选中时（取消选中）
            setForeground(list.getForeground());		//设置前景色（文字颜色）为黑色
            setBackground(list.getBackground());		//设置背景色为白色
        }
        return this;
    }
}
