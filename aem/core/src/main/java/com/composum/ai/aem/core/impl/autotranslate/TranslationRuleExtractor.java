package com.composum.ai.aem.core.impl.autotranslate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.apache.sling.api.resource.Resource;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.text.csv.Csv;

/**
 * Reads translation rules from an xls document.
 * <p>
 * It's difficult to use POI since the AEM uber-jar 6.5.7 which we include does not contain the full POI library
 * and cannot read XLSX files. We use the fastexcel library instead.
 * </p>
 */
public class TranslationRuleExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationRuleExtractor.class);

    public Map<String, String> extractRules(Resource resource, int sheetIndexBase1, int startRowBase1,
                                            String keyColumnStr, String valueColumnStr) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("Could not find resource.");
        }
        if (sheetIndexBase1 < 1) {
            throw new IllegalArgumentException("Sheet index must be at least 1.");
        }
        if (startRowBase1 < 1) {
            throw new IllegalArgumentException("Start row must be at least 1.");
        }
        int keyColumn = keyColumnStr.toUpperCase().charAt(0) - 'A';
        int valueColumn = valueColumnStr.toUpperCase().charAt(0) - 'A';
        if (resource.getPath().contains(".csv")) {
            return extractRulesFromCsv(resource, sheetIndexBase1 - 1, startRowBase1 - 1, keyColumn, valueColumn);
        } else {
            return extractRulesFromXlsx(resource, sheetIndexBase1 - 1, startRowBase1 - 1, keyColumn, valueColumn);
        }
    }

    /**
     * Reads the data from a spreadsheet.
     * Unfortunately the fastexcel library does currently only support xlsx.
     *
     * @throws IOException if the file cannot be read, in particular it'll be a {@link ZipException} if it's not a valid xlsx file, e.g.
     *                     an .xls or .ods or other file.
     */
    protected Map<String, String> extractRulesFromXlsx(Resource resource, int sheetIndex, int startRow,
                                                       int keyColumn, int valueColumn) throws IOException {
        Map<String, String> rules = new LinkedHashMap<>();
        try (InputStream inputStream = getAssetInputStream(resource);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            Optional<Sheet> sheetOpt = workbook.getSheet(sheetIndex);
            if (sheetOpt.isPresent()) {
                Sheet sheet = sheetOpt.get();
                try (Stream<Row> rows = sheet.openStream()) {
                    rows.forEach(row -> {
                        if (row.getRowNum() <= startRow) { // getRowNum() starts with 1
                            return;
                        }
                        String key = row.getCellAsString(keyColumn).orElse(null);
                        String value = row.getCellAsString(valueColumn).orElse(null);
                        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                            rules.put(key, value);
                        }
                    });
                }
            } else {
                throw new IllegalArgumentException("Sheet at index " + sheetIndex + " not found in " + resource.getPath());
            }
        }
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No rules found in " + resource.getPath());
        }
        LOG.info("Extracted {} rules from {}", rules.size(), resource.getPath());
        return rules;
    }

    /**
     * Reads the data from a CSV file.
     */
    protected Map<String, String> extractRulesFromCsv(Resource resource, int sheetIndex, int startRow,
                                                      int keyColumn, int valueColumn) throws IOException {
        Csv csv = new Csv();
        try (InputStream inputStream = getAssetInputStream(resource)) {
            Iterator<String[]> data = csv.read(inputStream, "UTF-8");
            for (int i = 0; i < startRow; i++) {
                if (!data.hasNext()) {
                    throw new IllegalArgumentException("Not enough rows in CSV file " + resource.getPath());
                }
                data.next();
            }
            Map<String, String> rules = new LinkedHashMap<>();
            while (data.hasNext()) {
                String[] row = data.next();
                if (row.length > keyColumn && row.length > valueColumn) {
                    String key = row[keyColumn];
                    String value = row[valueColumn];
                    if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                        rules.put(key, value);
                    }
                }
            }
            return rules;
        }
    }

    protected InputStream getAssetInputStream(Resource xlsResource) throws NoSuchElementException {
        return Optional.ofNullable(xlsResource.adaptTo(Asset.class))
                .map(Asset::getOriginal)
                .map(Rendition::getStream)
                .orElseThrow(NoSuchElementException::new);
    }
}
