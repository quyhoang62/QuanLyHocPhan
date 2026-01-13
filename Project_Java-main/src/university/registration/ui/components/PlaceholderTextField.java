package university.registration.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * JTextField với hỗ trợ placeholder text
 */
public class PlaceholderTextField extends JTextField {
    private String placeholder;

    public PlaceholderTextField() {
        super();
    }

    public PlaceholderTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder == null || placeholder.length() == 0 || getText().length() > 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(156, 163, 175)); // Gray color for placeholder
        g2.setFont(getFont().deriveFont(Font.PLAIN));
        
        Insets insets = getInsets();
        int x = insets.left + 12;
        int y = (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
        
        g2.drawString(placeholder, x, y);
    }
}


