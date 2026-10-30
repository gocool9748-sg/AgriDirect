package com.agrimarket.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

/** Simple horizontal bar chart drawn with Java 2D (no chart library needed). */
public class BarChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private Map<String, ? extends Number> data = new LinkedHashMap<>();
    private final String emptyText;

    public BarChartPanel(String emptyText) {
        this.emptyText = emptyText;
        setOpaque(false);
        setPreferredSize(new Dimension(420, 220));
    }

    public void setData(Map<String, ? extends Number> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(UI.font(Font.PLAIN, 13));
        FontMetrics fm = g2.getFontMetrics();

        if (data.isEmpty()) {
            g2.setColor(UI.MUTED);
            g2.drawString(emptyText, 4, 24);
            g2.dispose();
            return;
        }

        double max = data.values().stream().mapToDouble(Number::doubleValue).max().orElse(1);
        int labelWidth = 0;
        for (String k : data.keySet()) {
            labelWidth = Math.max(labelWidth, fm.stringWidth(k));
        }
        labelWidth += 12;
        int valueWidth = 48;
        int rows = data.size();
        int rowHeight = Math.max(24, Math.min(40, (getHeight() - 8) / rows));
        int barHeight = Math.max(12, rowHeight - 14);
        int barMax = Math.max(40, getWidth() - labelWidth - valueWidth);

        int y = 4;
        for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
            int textY = y + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(UI.GREEN);
            g2.drawString(e.getKey(), 0, textY);
            int w = (int) Math.round(barMax * (e.getValue().doubleValue() / max));
            g2.setColor(UI.ORANGE);
            g2.fillRoundRect(labelWidth, y + (rowHeight - barHeight) / 2, Math.max(w, 3), barHeight, 6, 6);
            g2.setColor(UI.GREEN);
            g2.setFont(UI.font(Font.BOLD, 13));
            g2.drawString(String.valueOf(e.getValue()), labelWidth + w + 8, textY);
            g2.setFont(UI.font(Font.PLAIN, 13));
            y += rowHeight;
        }
        g2.dispose();
    }
}
