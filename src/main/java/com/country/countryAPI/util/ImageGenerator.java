package com.country.countryAPI.util;

import com.country.countryAPI.model.Country;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class ImageGenerator {
    private static final String IMAGE_PATH = "cache/summary.png";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());
    public void generateSummaryImage(int totalCountries, Instant lastRefreshedAt, List<Country> topFiveGdp) {
        int width = 800;
        int height = 500;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(30, 144, 255));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2d.drawString("Country Data Cache Summary", 40, 50);

        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2d.drawString("Total Countries Cached: " + totalCountries, 40, 100);
        g2d.drawString("Last Refresh: " + FORMATTER.format(lastRefreshedAt), 40, 130);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Top 5 Countries by Estimated GDP (USD)", 40, 200);

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 14));
        int y = 230;

        g2d.setColor(new Color(100, 100, 100));
        g2d.drawString(String.format("%-4s %-25s %-10s %s", "#", "Country Name", "Currency", "Estimated GDP"), 40, y);
        y += 15;
        g2d.drawLine(40, y, width - 40, y);
        y += 10;

        g2d.setColor(Color.BLACK);

        for (int i = 0; i < topFiveGdp.size(); i++) {
            Country c = topFiveGdp.get(i);
            String name = c.getName();
            String code = c.getCurrencyCode() != null ? c.getCurrencyCode() : "N/A";

            String formattedGdp = formatGdp(c.getEstimatedGdp());

            String line = String.format("%-4d %-25s %-10s %s", i + 1, name, code, formattedGdp);
            g2d.drawString(line, 40, y);
            y += 25;
        }
        g2d.dispose();
        try {
            Path filePath = Path.of(IMAGE_PATH);
            Files.createDirectories(filePath.getParent());
            File outputFile = filePath.toFile();
            ImageIO.write(image, "png", outputFile);
            log.info("Successfully generated summary image at: {}", IMAGE_PATH);
        } catch (IOException e) {
            log.error("Failed to write summary image to disk.", e);
        }
    }
    private String formatGdp(BigDecimal gdp) {
        if (gdp == null || gdp.doubleValue() == 0.0) return "N/A";
        double absGdp = gdp.doubleValue();
        if (absGdp >= 1_000_000_000_000.0) {
            return String.format("$%.2fT", absGdp / 1_000_000_000_000.0);
        } else if (absGdp >= 1_000_000_000.0) {
            return String.format("$%.2fB", absGdp / 1_000_000_000.0);
        } else if (absGdp >= 1_000_000.0) {
            return String.format("$%.2fM", absGdp / 1_000_000.0);
        } else {
            return String.format("$%.2f", absGdp);
        }
    }
}
