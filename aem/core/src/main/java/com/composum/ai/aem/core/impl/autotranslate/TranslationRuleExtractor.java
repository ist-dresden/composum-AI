package com.composum.ai.aem.core.impl.autotranslate;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

/**
 * Extracts translation rules from excel documents.
 */
public class TranslationRuleExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationRuleExtractor.class);

    /**
     * Collects a mapping from key to value from the given excel resource, starting from the given row, and using the given columns for key and value.
     */
    public Map<String, String> extractRules(Resource xlsResource, int sheetIndex, int startRow, int keyColumn, int valueColumn) throws IOException {
        Map<String, String> rules = new LinkedHashMap<>();
        try (InputStream inputStream = getAssetInputStream(xlsResource)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String key = row.getCell(keyColumn).getStringCellValue();
                String value = row.getCell(valueColumn).getStringCellValue();
                if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                    rules.put(key, value);
                }
            }

        }
        LOG.info("Extracted {} rules from {}", rules.size(), xlsResource.getPath());
        return rules;
    }

    protected InputStream getAssetInputStream(Resource xlsResource)
            throws NoSuchElementException {
        return Optional.ofNullable(xlsResource.adaptTo(Asset.class))
                .map(Asset::getOriginal)
                .map(Rendition::getStream)
                .orElseThrow(NoSuchElementException::new);
    }

}
