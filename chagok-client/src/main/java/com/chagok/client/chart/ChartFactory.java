package com.chagok.client.chart;

import com.chagok.client.util.UiFonts;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class ChartFactory {
    private static final Color[] PIE = {
            new Color(0x1F6F6A), new Color(0xE07A3D), new Color(0x3D7EA6),
            new Color(0xC45C6A), new Color(0x7A6A3D), new Color(0x8AA399)
    };
    private static final Color MINE = new Color(0x1F6F6A);
    private static final Color PEER = new Color(0xC5C1B7);
    private static final Color PREV = new Color(0xB7D4C8);
    private static final Color CURR = new Color(0x1F6F6A);

    private ChartFactory() {
    }

    public static ChartPanel pie(List<CategoryStatDTO> items) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (CategoryStatDTO item : items) {
            dataset.setValue(item.getName(), item.getAmount());
        }
        JFreeChart chart = org.jfree.chart.ChartFactory.createPieChart("카테고리별 지출", dataset, true, true, false);
        applyFont(chart);
        PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
        NumberFormat percent = NumberFormat.getPercentInstance(Locale.KOREA);
        percent.setMinimumFractionDigits(1);
        percent.setMaximumFractionDigits(1);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} {2}",
                NumberFormat.getIntegerInstance(Locale.KOREA), percent));
        plot.setLabelFont(UiFonts.ui(12));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        for (int i = 0; i < items.size(); i++) {
            plot.setSectionPaint(items.get(i).getName(), PIE[i % PIE.length]);
        }
        return panel(chart);
    }

    public static ChartPanel monthBars(MonthlyStatDTO previous, MonthlyStatDTO current) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        add(dataset, "전월", previous);
        add(dataset, "당월", current);
        JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
                "전월 대비", "", "원", dataset, PlotOrientation.VERTICAL, true, true, false);
        applyFont(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, PREV);
        renderer.setSeriesPaint(1, CURR);
        return panel(chart);
    }

    public static ChartPanel peerBars(List<CategoryStatDTO> items) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int limit = Math.min(6, items.size());
        for (int i = 0; i < limit; i++) {
            CategoryStatDTO item = items.get(i);
            if (item.getAmount() == 0 && (item.getPeerAvg() == null || item.getPeerAvg() == 0)) {
                continue;
            }
            dataset.addValue(item.getAmount(), "나", item.getName());
            dataset.addValue(item.getPeerAvg() == null ? 0 : item.getPeerAvg(), "또래 평균", item.getName());
        }
        JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
                "같은 연령대 비교", "", "원", dataset, PlotOrientation.VERTICAL, true, true, false);
        applyFont(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, MINE);
        renderer.setSeriesPaint(1, PEER);
        return panel(chart);
    }

    private static void add(DefaultCategoryDataset dataset, String series, MonthlyStatDTO stat) {
        dataset.addValue(stat.getIncome(), series, "수입");
        dataset.addValue(stat.getFixed(), series, "고정지출");
        dataset.addValue(stat.getVariable(), series, "유동지출");
    }

    private static void applyFont(JFreeChart chart) {
        Font font = UiFonts.ui(12);
        Font title = UiFonts.ui(Font.BOLD, 15);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(title);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(font);
        }
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getPlot() instanceof CategoryPlot plot) {
            plot.getDomainAxis().setTickLabelFont(font);
            plot.getDomainAxis().setLabelFont(font);
            plot.getRangeAxis().setTickLabelFont(font);
            plot.getRangeAxis().setLabelFont(font);
        }
    }

    private static ChartPanel panel(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setBackground(Color.WHITE);
        panel.setMaximumDrawWidth(1600);
        panel.setMaximumDrawHeight(900);
        return panel;
    }
}
