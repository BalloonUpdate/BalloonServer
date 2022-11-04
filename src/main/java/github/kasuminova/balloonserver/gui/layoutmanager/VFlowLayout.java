package github.kasuminova.balloonserver.gui.layoutmanager;

import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * MyVFlowLayout is similar to FlowLayout except it lays out components
 * vertically. Extends FlowLayout because it mimics much of the behavior of the
 * FlowLayout class, except vertically. An additional feature is that you can
 * specify a fill to edge flag, which causes the MyVFlowLayout manager to
 * resize all components to expand to the column width Warning: This causes
 * problems when the main panel has less space that it needs and it seems to
 * prohibit multi-column output. Additionally there is a vertical fill flag,
 * which fills the last component to the remaining height of the container.
 */
public class VFlowLayout extends FlowLayout {
    /**
     * Specify alignment top.
     */
    public static final int TOP = 0;
    /**
     * Specify a middle alignment.
     */
    public static final int MIDDLE = 1;
    /**
     * Specify the alignment to be bottom.
     */
    public static final int BOTTOM = 2;
    /**
     * Specify the alignment to be left.
     */
    public static final int LEFT = 0;
    /**
     * Specify the alignment to be right.
     */
    public static final int RIGHT = 2;
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 1L;
    private int horizontalAlignment;
    private int topVerticalGap;
    private int bottomVerticalGap;
    private boolean isHorizontalFill;
    private boolean isVerticalFill;

    public VFlowLayout() {
        this(TOP, MIDDLE, 5, 5, 5, 5, true, false);
    }

    public VFlowLayout(boolean isHorizontalFill, boolean isVerticalFill) {
        this(TOP, MIDDLE, 5, 5, 5, 5, isHorizontalFill, isVerticalFill);
    }

    public VFlowLayout(int align) {
        this(align, MIDDLE, 5, 5, 5, 5, true, false);
    }

    public VFlowLayout(int align, boolean isHorizontalFill, boolean isVerticalFill) {
        this(align, MIDDLE, 5, 5, 5, 5, isHorizontalFill, isVerticalFill);
    }

    public VFlowLayout(int align, int horizontalGap, int verticalGap, boolean isHorizontalFill, boolean isVerticalFill) {
        this(align, MIDDLE, horizontalGap, verticalGap, verticalGap, verticalGap, isHorizontalFill, isVerticalFill);
    }

    public VFlowLayout(int align, int horizontalGap, int verticalGap, int topVerticalGap, int bottomVerticalGap, boolean isHorizontalFill, boolean isVerticalFill) {
        this(align, MIDDLE, horizontalGap, verticalGap, topVerticalGap, bottomVerticalGap, isHorizontalFill, isVerticalFill);
    }

    /**
     * Construct a new MyVFlowLayout.
     *
     * @param verticalAlignment   the alignment value
     * @param horizontalAlignment the horizontal alignment value
     * @param horizontalGap       the horizontal gap variable
     * @param verticalGap         the vertical gap variable
     * @param topVerticalGap      the top vertical gap variable
     * @param bottomVerticalGap   the bottom vertical gap variable
     * @param isHorizontalFill    the fill to edge flag
     * @param isVerticalFill      true if the panel should vertically fill.
     */
    public VFlowLayout(int verticalAlignment, int horizontalAlignment, int horizontalGap, int verticalGap, int topVerticalGap, int bottomVerticalGap, boolean isHorizontalFill, boolean isVerticalFill) {
        this.setAlignment(verticalAlignment);
        this.setHorizontalAlignment(horizontalAlignment);
        this.setHgap(horizontalGap);
        this.setVgap(verticalGap);
        this.topVerticalGap = topVerticalGap;
        this.bottomVerticalGap = bottomVerticalGap;
        this.isHorizontalFill = isHorizontalFill;
        this.isVerticalFill = isVerticalFill;
    }

    public int getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        if (LEFT == horizontalAlignment) {
            this.horizontalAlignment = LEFT;
        } else if (RIGHT == horizontalAlignment) {
            this.horizontalAlignment = RIGHT;
        } else {
            this.horizontalAlignment = MIDDLE;
        }
    }

    @Override
    public void setHgap(int horizontalGap) {
        super.setHgap(horizontalGap);
    }

    @Override
    public void setVgap(int verticalGap) {
        super.setVgap(verticalGap);
    }

    public int getTopVerticalGap() {
        return this.topVerticalGap;
    }

    public void setTopVerticalGap(int topVerticalGap) {
        this.topVerticalGap = topVerticalGap;
    }

    public int getBottomVerticalGap() {
        return this.bottomVerticalGap;
    }

    public void setBottomVerticalGap(int bottomVerticalGap) {
        this.bottomVerticalGap = bottomVerticalGap;
    }

    /**
     * Returns true if the layout vertically fills.
     *
     * @return true if vertically fills the layout using the specified.
     */
    public boolean getVerticalFill() {
        return isVerticalFill;
    }

    /**
     * Set true to fill vertically.
     *
     * @param isVerticalFill true to fill vertically.
     */
    public void setVerticalFill(boolean isVerticalFill) {
        this.isVerticalFill = isVerticalFill;
    }

    /**
     * Returns true if the layout horizontally fills.
     *
     * @return true if horizontally fills.
     */
    public boolean getHorizontalFill() {
        return isHorizontalFill;
    }

    /**
     * Set to true to enable horizontally fill.
     *
     * @param isHorizontalFill true to fill horizontally.
     */
    public void setHorizontalFill(boolean isHorizontalFill) {
        this.isHorizontalFill = isHorizontalFill;
    }

    /**
     * Returns the preferred dimensions given the components in the target
     * container.
     *
     * @param container the component to lay out
     */
    @Override
    public Dimension preferredLayoutSize(Container container) {
        Dimension rs = new Dimension(0, 0);
        List<Component> components = VFlowLayout.getVisibleComponents(container);
        Dimension dimension = this.preferredComponentsSize(components);
        rs.width += dimension.width;
        rs.height += dimension.height;
        Insets insets = container.getInsets();
        rs.width += insets.left + insets.right;
        rs.height += insets.top + insets.bottom;

        if (!components.isEmpty()) {
            rs.width += this.getHgap() * 2;
            rs.height += this.topVerticalGap;
            rs.height += this.bottomVerticalGap;
        }

        return rs;
    }

    /**
     * Returns the minimum size needed to layout the target container.
     *
     * @param container the component to lay out.
     * @return the minimum layout dimension.
     */
    @Override
    public Dimension minimumLayoutSize(Container container) {
        Dimension rs = new Dimension(0, 0);
        List<Component> components = VFlowLayout.getVisibleComponents(container);
        Dimension dimension = this.minimumComponentsSize(components);
        rs.width += dimension.width;
        rs.height += dimension.height;
        Insets insets = container.getInsets();
        rs.width += insets.left + insets.right;
        rs.height += insets.top + insets.bottom;

        if (!components.isEmpty()) {
            rs.width += this.getHgap() * 2;
            rs.height += this.topVerticalGap;
            rs.height += this.bottomVerticalGap;
        }

        return rs;
    }

    @Override
    public void layoutContainer(Container container) {
        int horizontalGap = this.getHgap();
        int verticalGap = this.getVgap();
        Insets insets = container.getInsets();
        int maxWidth = container.getSize().width - (insets.left + insets.right + horizontalGap * 2);
        int maxHeight = container.getSize().height - (insets.top + insets.bottom + this.topVerticalGap + this.bottomVerticalGap);
        List<Component> components = VFlowLayout.getVisibleComponents(container);
        Dimension preferredComponentsSize = this.preferredComponentsSize(components);
        int alignment = this.getAlignment();
        int y = insets.top + this.topVerticalGap;

        if (!this.isVerticalFill && preferredComponentsSize.height < maxHeight) {
            if (MIDDLE == alignment) {
                y += (maxHeight - preferredComponentsSize.height) / 2;
            } else if (BOTTOM == alignment) {
                y += maxHeight - preferredComponentsSize.height;
            }
        }

        int index = 0;

        for (Component component : components) {
            int x = insets.left + horizontalGap;
            Dimension dimension = component.getPreferredSize();

            if (this.isHorizontalFill) {
                dimension.width = maxWidth;
            } else {
                dimension.width = Math.min(maxWidth, dimension.width);

                if (MIDDLE == this.horizontalAlignment) {
                    x += (maxWidth - dimension.width) / 2;
                } else if (RIGHT == this.horizontalAlignment) {
                    x += maxWidth - dimension.width;
                }
            }

            if (this.isVerticalFill && index == components.size() - 1) {
                int height = maxHeight + this.topVerticalGap + insets.top - y;
                dimension.height = Math.max(height, dimension.height);
            }

            component.setSize(dimension);
            component.setLocation(x, y);
            y += dimension.height + verticalGap;
            index++;
        }
    }

    private Dimension preferredComponentsSize(List<Component> components) {
        Dimension rs = new Dimension(0, 0);

        for (Component component : components) {
            Dimension dimension = component.getPreferredSize();
            rs.width = Math.max(rs.width, dimension.width);
            rs.height += dimension.height;
        }

        if (!components.isEmpty()) {
            rs.height += this.getVgap() * (components.size() - 1);
        }

        return rs;
    }

    private Dimension minimumComponentsSize(List<Component> components) {
        Dimension rs = new Dimension(0, 0);

        for (Component component : components) {
            Dimension dimension = component.getMinimumSize();
            rs.width = Math.max(rs.width, dimension.width);
            rs.height += dimension.height;
        }

        if (!components.isEmpty()) {
            rs.height += this.getVgap() * (components.size() - 1);
        }

        return rs;
    }

    private static List<Component> getVisibleComponents(Container container) {
        List<Component> rs = new ArrayList<>();

        for (Component component : container.getComponents()) {
            if (component.isVisible()) {
                rs.add(component);
            }
        }

        return rs;
    }
}