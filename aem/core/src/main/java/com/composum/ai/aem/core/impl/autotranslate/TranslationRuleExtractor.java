package com.composum.ai.aem.core.impl.autotranslate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipException;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.sling.api.resource.Resource;
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

    /**
     * Extracts translation rules from a spreadsheet.
     *
     * @param resource        the resource containing the spreadsheet (either a file resource or an AEM asset)
     * @param sheetIndexBase1 the index of the sheet in the XLS file containing the translation table. The first sheet is number 1.
     *                        Ignored for CSV files.
     * @param startRowBase1   the row in the sheet where the translation table starts. The first row is 1, following Excel conventions.
     * @param keyColumnStr    the column in the sheet containing the keys (terms to be translated).
     *                        The first column is A (following Excel conventions) or 1.
     * @param valueColumnStr  the column in the sheet containing the values (translations).
     *                        The first column is A (following Excel conventions) or 1.
     * @return a map of key-value pairs
     * @throws IOException if the file cannot be read, in particular it'll be a {@link ZipException} if it's not a valid xlsx file, e.g.
     */
    @Nonnull
    public Map<String, String> extractRules(@Nonnull Resource resource, int sheetIndexBase1, int startRowBase1,
                                            @Nonnull String keyColumnStr, @Nonnull String valueColumnStr) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("Could not find resource.");
        }
        if (sheetIndexBase1 < 1) {
            throw new IllegalArgumentException("Sheet index must be at least 1.");
        }
        if (startRowBase1 < 1) {
            throw new IllegalArgumentException("Start row must be at least 1.");
        }
        int keyColumn = parseColumnName(keyColumnStr);
        int valueColumn = parseColumnName(valueColumnStr);
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
    public Map<String, String> extractRulesFromXlsx(Resource xlsResource, int sheetIndex, int startRow, int keyColumn, int valueColumn) throws IOException {
        Map<String, String> rules = new LinkedHashMap<>();
        try (InputStream inputStream = getAssetInputStream(xlsResource)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet at index " + sheetIndex + " not found in " + xlsResource.getPath());
            }
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell keyCell = row.getCell(keyColumn);
                    String key = keyCell != null ? keyCell.getStringCellValue() : null;
                    Cell valueCell = row.getCell(valueColumn);
                    String value = valueCell != null ? valueCell.getStringCellValue() : null;
                    if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                        rules.put(key, value);
                    }
                }
            }

        }
        LOG.info("Extracted {} rules from {}", rules.size(), xlsResource.getPath());
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No rules found in " + xlsResource.getPath());
        }
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
                        rules.put(normalize(key), normalize(value));
                    }
                }
            }
            return rules;
        }
    }

    protected String normalize(String s) {
        return s.trim().replace("\\s+", " ");
    }

    protected InputStream getAssetInputStream(Resource xlsResource) throws NoSuchElementException {
        return Optional.ofNullable(xlsResource.adaptTo(Asset.class))
                .map(Asset::getOriginal)
                .map(Rendition::getStream)
                .orElseThrow(NoSuchElementException::new);
    }

    protected int parseColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("Empty column name");
        }
        if (columnName.matches("\\d+")) {
            return Integer.parseInt(columnName) - 1;
        }
        int column = 0;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                column = column * 26 + c - 'A' + 1;
            } else if (c >= 'a' && c <= 'z') {
                column = column * 26 + c - 'a' + 1;
            } else {
                throw new IllegalArgumentException("Invalid column name: " + columnName);
            }
        }
        return column - 1;
    }

}
