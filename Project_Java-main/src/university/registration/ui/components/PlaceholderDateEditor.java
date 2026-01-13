package university.registration.ui.components;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Custom DateEditor cho JSpinner với hỗ trợ placeholder
 */
public class PlaceholderDateEditor extends JSpinner.DateEditor {
    private String placeholder;
    private JFormattedTextField textField;

    public PlaceholderDateEditor(JSpinner spinner, String dateFormatPattern, String placeholder) {
        super(spinner, dateFormatPattern);
        this.placeholder = placeholder;
        this.textField = getTextField();
        
        // Tạo custom text field với placeholder
        JFormattedTextField customField = new JFormattedTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                String text = getText();
                if ((placeholder != null && placeholder.length() > 0) && 
                    (text == null || text.trim().isEmpty())) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(156, 163, 175)); // Gray color for placeholder
                    g2.setFont(getFont().deriveFont(Font.PLAIN));
                    
                    Insets insets = getInsets();
                    int x = insets.left + 12;
                    int y = (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
                    
                    g2.drawString(placeholder, x, y);
                    g2.dispose();
                }
            }
        };
        
        // Copy properties từ textField hiện tại
        customField.setFormatterFactory(textField.getFormatterFactory());
        customField.setValue(textField.getValue());
        customField.setEditable(false);
        customField.setFont(textField.getFont());
        customField.setBorder(textField.getBorder());
        customField.setBackground(textField.getBackground());
        customField.setForeground(textField.getForeground());
        
        // Thay thế textField
        remove(textField);
        add(customField, BorderLayout.CENTER);
        this.textField = customField;
    }
    
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        if (textField != null) {
            textField.repaint();
        }
    }
    
    public String getPlaceholder() {
        return placeholder;
    }
}


