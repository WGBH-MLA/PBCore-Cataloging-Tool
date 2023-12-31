package digitalbedrock.software.pbcore.parsers;

import static com.opencsv.ICSVWriter.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.core.models.NewDocumentType;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreAttribute;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreStructure;

public class CSVPBCoreParser {

    public static final Logger LOGGER = Logger.getLogger(CSVPBCoreParser.class.getName());

    public static void writeFile(Map<String, PBCoreElement> mapPbCoreElements, String csvFile) {

        List<CSVElementMapper> csvElementMappers = MainApp.getInstance().getRegistry().loadMappers();
        try (Writer writer = Files.newBufferedWriter(Paths.get(csvFile));
                CSVWriter csvWriter = new CSVWriter(writer, DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER,
                        DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END)) {
            final String[] headerRecord = getHeaderRecords(csvElementMappers);

            csvWriter.writeNext(headerRecord);
            mapPbCoreElements.forEach((key, pbCoreElement) -> {
                List<String> records = new ArrayList<>();
                setIndexesForElement(pbCoreElement);
                for (String s : headerRecord) {
                    PBCoreElement pbCoreE = mapPBCoreElementToString(pbCoreElement, s, csvElementMappers);
                    if (pbCoreE == null) {
                        PBCoreAttribute pbCoreAttribute = mapPBCoreAttributeString(pbCoreElement, s, csvElementMappers);
                        if (pbCoreAttribute != null) {
                            records.add(pbCoreAttribute.getValue());
                        }
                        else {
                            records.add("");
                        }
                    }
                    else {
                        records.add(pbCoreE.getValue());
                    }
                }
                csvWriter.writeNext(records.toArray(new String[records.size()]));
            });
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "error writing file", e);
        }
    }

    private static void setIndexesForElement(PBCoreElement pbCoreElement) {

        Map<String, Integer> map = new HashMap<>();
        for (PBCoreElement coreElement : pbCoreElement.getSubElements()) {
            if (!map.containsKey(coreElement.getFullPath())) {
                map.put(coreElement.getFullPath(), 0);
            }
            map.put(coreElement.getFullPath(), map.get(coreElement.getFullPath()) + 1);
            coreElement.setIndex(map.get(coreElement.getFullPath()));
            setIndexesForElement(coreElement);
            setIndexesForElementAttributes(coreElement.getAttributes());
        }
        setIndexesForElementAttributes(pbCoreElement.getAttributes());
    }

    private static void setIndexesForElementAttributes(List<PBCoreAttribute> pbCoreAttributes) {

        Map<String, Integer> map = new HashMap<>();
        for (PBCoreAttribute coreAttribute : pbCoreAttributes) {
            if (!map.containsKey(coreAttribute.getFullPath())) {
                map.put(coreAttribute.getFullPath(), 0);
            }
            map.put(coreAttribute.getFullPath(), map.get(coreAttribute.getFullPath()) + 1);
            coreAttribute.setIndex(map.get(coreAttribute.getFullPath()));
        }
    }

    private static String[] getHeaderRecords(List<CSVElementMapper> csvElementMappers) {

        List<String> headers = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        csvElementMappers.forEach(em -> map.put(em.getName(), 1));
        csvElementMappers.forEach(csvElementMapper -> {
            int index = map.get(csvElementMapper.getName());
            headers.add(csvElementMapper.getName() + index);
            csvElementMapper.getAttributes().forEach(am -> headers.add(am.getName() + index));
            map.put(csvElementMapper.getName(), map.get(csvElementMapper.getName()) + 1);

        });
        return headers.toArray(new String[headers.size()]);
    }

    public static List<PBCoreElement> parseFile(String csvFile) throws IOException, CsvValidationException {

        List<CSVElementMapper> csvElementMappers = MainApp.getInstance().getRegistry().loadMappers();
        List<PBCoreElement> pbCoreElements = new ArrayList<>();

        CSVReader csvReader;
        String[] nextRecord;
        List<String> mappedFullPaths;
        try (Reader reader = Files.newBufferedReader(Paths.get(csvFile))) {
            csvReader = new CSVReader(reader);
            mappedFullPaths = Arrays.asList(csvReader.readNext());
            while ((nextRecord = csvReader.readNext()) != null) {
                PBCoreElement copy = PBCoreStructure
                        .getInstance()
                        .getRootElement(NewDocumentType.DESCRIPTION_DOCUMENT)
                        .copy(true);
                copy.clearOptionalSubElements();
                List<PBCoreElement> elements = new ArrayList<>(copy.getSubElements());
                copy.getSubElements().clear();
                int c = 0;
                for (String value : nextRecord) {
                    if (mappedFullPaths.size() <= c) {
                        break;
                    }
                    String s = mappedFullPaths.get(c++).replaceAll("\uFEFF", "");
                    PBCoreElement pbCoreElement = mapStringToPBCoreElement(copy, s, value, csvElementMappers);
                    if (pbCoreElement == null) {
                        mapStringToPBCoreAttribute(copy, s, value, csvElementMappers);
                    }
                    else {
                        int i1 = pbCoreElement.getFullPath().lastIndexOf("/");
                        if (!pbCoreElement.getFullPath().substring(0, i1).equals(copy.getFullPath())) {
                            pbCoreElement = getRootElementUntilCopyFullpath(copy.getFullPath(), pbCoreElement);
                        }
                        PBCoreElement finalPbCoreElement = pbCoreElement;
                        if (copy
                                .getSubElements()
                                .stream()
                                .noneMatch(pbc -> pbc.getFullPath().equalsIgnoreCase(finalPbCoreElement.getFullPath())
                                        && pbc.getIndex() == finalPbCoreElement.getIndex())) {
                            copy.addSubElement(pbCoreElement);
                        }
                    }
                }
                elements
                        .stream()
                        .filter(pbe -> pbe.isRequired()
                                && !copy.hasAtLeastOneElementWithSameNameAndFilledValue(pbe.getName()))
                        .forEach(copy::addSubElement);
                pbCoreElements.add(copy);
            }
        }
        fillAllElementsWithRequiredSubElementsAndAttributes(pbCoreElements);
        return pbCoreElements;
    }

    private static void fillAllElementsWithRequiredSubElementsAndAttributes(List<PBCoreElement> pbCoreElements) {

        List<PBCoreElement> elementsToProcess = new ArrayList<>(pbCoreElements);
        while (!elementsToProcess.isEmpty()) {
            PBCoreElement pbCoreElement = elementsToProcess.remove(0);
            PBCoreElement elementFromStructure = PBCoreStructure.getInstance().getElement(pbCoreElement.getFullPath());

            for (PBCoreElement coreElement : elementFromStructure.getRequiredSubElements()) {
                if (pbCoreElement
                        .getRequiredSubElements()
                        .stream()
                        .noneMatch(pbe -> pbe.getFullPath().equals(coreElement.getFullPath()))) {
                    pbCoreElement.addSubElement(coreElement.copy(false));
                }
            }

            List<PBCoreAttribute> collect = elementFromStructure
                    .getAttributes()
                    .stream()
                    .filter(PBCoreAttribute::isRequired)
                    .collect(Collectors.toList());
            for (PBCoreAttribute pbCoreAttribute : collect) {
                if (pbCoreElement
                        .getAttributes()
                        .stream()
                        .noneMatch(pba -> pba.getFullPath().equals(pbCoreAttribute.getFullPath()))) {
                    pbCoreElement.addAttribute(pbCoreAttribute.copy(false));
                }
            }
            elementsToProcess.addAll(pbCoreElement.getOrderedSubElements());
        }
    }

    private static PBCoreElement getRootElementUntilCopyFullpath(String copyFullPath, PBCoreElement pbCoreElement) {

        PBCoreElement element = pbCoreElement;
        int i1 = element.getFullPath().lastIndexOf("/");
        while (i1 != -1) {
            String str = element.getFullPath().substring(0, i1);
            if (str.equals(copyFullPath)) {
                break;
            }
            PBCoreElement elementToAdd = element;
            element = PBCoreStructure.getInstance().getElement(str).copy(false);
            element.clearAllEmptyElementsAndAttributes();
            element.addSubElement(elementToAdd);
            i1 = element.getFullPath().lastIndexOf("/");
        }
        return element;

    }

    private static PBCoreElement mapStringToPBCoreElement(PBCoreElement rootElement, String key, String value,
                                                          List<CSVElementMapper> mappers) {

        PBCoreElement element = null;
        List<String> strings = mappers.stream().map(CSVElementMapper::getName).collect(Collectors.toList());
        String string = "";
        int index = -1;
        for (String s : strings) {
            if (key.startsWith(s)) {
                try {
                    String[] split = key.split(s);
                    if (split.length < 2) {
                        index = 1;
                    }
                    else {
                        index = Integer.parseInt(key.split(s)[1]);
                    }
                    string = s;
                    break;
                }
                catch (NumberFormatException e) {
                }
            }
        }
        String finalString = string;
        CSVElementMapper csvElementMapper = mappers
                .stream()
                .filter(em -> em.getName().equals(finalString))
                .findFirst()
                .orElse(null);
        if (csvElementMapper != null) {
            if (csvElementMapper.isNeedsParentVerification()) {
                element = verifyElement(rootElement, csvElementMapper.getParentElementFullPath(), index);
                /*if (element != null && !element.isRequired() && (value == null || value.trim().isEmpty())) {
                    return null;
                }*/
                PBCoreElement element1 = PBCoreStructure
                        .getInstance()
                        .getElement(csvElementMapper.getElementFullPath())
                        .copy(false);
                element1.setValue(value);
                element1.setValid(element1.getValue() != null && !element1.getValue().trim().isEmpty());
                element1.setIndex(index);
                element.setIndex(index);
                element.addSubElement(element1);
                return element;
            }
            else {
                PBCoreElement element1 = PBCoreStructure
                        .getInstance()
                        .getElement(csvElementMapper.getElementFullPath());
                element = element1.copy(false);
            }
        }
        if (element != null) {
            element.setValue(value);
            element.setValid(element.getValue() != null && !element.getValue().trim().isEmpty());
            element.setIndex(index);
        }
        return element;
    }

    private static PBCoreElement verifyElement(PBCoreElement rootElement, String fullPathToVerify, int index) {

        PBCoreElement element = rootElement
                .getSubElements()
                .stream()
                .filter(pbc -> pbc.getFullPath().equalsIgnoreCase(fullPathToVerify) && pbc.getIndex() == index)
                .findFirst()
                .orElse(null);
        if (element == null) {
            PBCoreElement element1 = PBCoreStructure.getInstance().getElement(fullPathToVerify);
            element = element1.copy(false);
            element.getSubElements().clear();
            element.getAttributes().clear();
        }
        return element;
    }

    private static PBCoreElement processElementAttribute(PBCoreElement rootElement, String elem, String attr,
                                                         String value, int index) {

        PBCoreElement element = findElement(rootElement, elem, index);
        if (element != null) {
            PBCoreAttribute pbCoreAttribute = element
                    .getAttributes()
                    .stream()
                    .filter(pbca -> pbca.getFullPath().equalsIgnoreCase(attr))
                    .findFirst()
                    .orElse(null);
            if (pbCoreAttribute != null) {
                pbCoreAttribute.setValue(value);
            }
            else {
                PBCoreAttribute pbCoreAttribute2 = PBCoreStructure
                        .getInstance()
                        .getElement(elem)
                        .getAttributes()
                        .stream()
                        .filter(at -> at.getFullPath().equals(attr))
                        .findFirst()
                        .orElse(null);
                PBCoreAttribute pbCoreAttribute1 = pbCoreAttribute2.copy();
                pbCoreAttribute1.setValue(value);
                element.addAttribute(pbCoreAttribute1);
            }
        }
        return element;
    }

    private static PBCoreElement mapStringToPBCoreAttribute(PBCoreElement rootElement, String key, String value,
                                                            List<CSVElementMapper> mappers) {

        List<String> strings = new ArrayList<>();
        mappers.forEach(em -> em.getAttributes().forEach(am -> strings.add(am.getName())));
        String string = "";
        int index = -1;
        for (String s : strings) {
            if (key.startsWith(s)) {
                try {
                    String[] split = key.split(s);
                    if (split.length < 2) {
                        index = 1;
                    }
                    else {
                        index = Integer.parseInt(key.split(s)[1]);
                    }
                    string = s;
                    break;
                }
                catch (NumberFormatException e) {
                }
            }
        }
        String finalString = string;
        CSVAttributeMapper attributeMapper = null;
        for (CSVElementMapper mapper : mappers) {
            attributeMapper = mapper
                    .getAttributes()
                    .stream()
                    .filter(am -> am.getName().equals(finalString))
                    .findFirst()
                    .orElse(null);
            if (attributeMapper != null) {
                break;
            }
        }
        if (attributeMapper != null) {
            return processElementAttribute(rootElement, attributeMapper.getElementFullPath(),
                                           attributeMapper.getAttributeFullPath(), value, index);
        }
        return null;
    }

    private static PBCoreAttribute mapPBCoreAttributeString(PBCoreElement pbCoreElement, String key,
                                                            List<CSVElementMapper> mappers) {

        List<String> strings = new ArrayList<>();
        mappers.forEach(em -> em.getAttributes().forEach(am -> strings.add(am.getName())));
        String string = "";
        int index = -1;
        for (String s : strings) {
            if (key.startsWith(s)) {
                try {
                    index = Integer.parseInt(key.split(s)[1]);
                    string = s;
                    break;
                }
                catch (NumberFormatException e) {
                }
            }
        }
        String finalString = string;
        CSVAttributeMapper attributeMapper = null;
        for (CSVElementMapper mapper : mappers) {
            attributeMapper = mapper
                    .getAttributes()
                    .stream()
                    .filter(am -> am.getName().equals(finalString))
                    .findFirst()
                    .orElse(null);
            if (attributeMapper != null) {
                break;
            }
        }
        if (attributeMapper != null) {
            PBCoreElement element = findElement(pbCoreElement, attributeMapper.getElementFullPath(), index);
            CSVAttributeMapper finalAttributeMapper = attributeMapper;
            return element == null ? null
                    : element
                            .getAttributes()
                            .stream()
                            .filter(pbca -> pbca
                                    .getFullPath()
                                    .equalsIgnoreCase(finalAttributeMapper.getAttributeFullPath()))
                            .findFirst()
                            .orElse(null);
        }
        return null;
    }

    private static PBCoreElement findElement(PBCoreElement pbCoreElement, String fullpath, int index) {

        List<String> split = new ArrayList<>(Arrays.asList(fullpath.split("/")));
        List<PBCoreElement> elementsToProcess = Arrays.asList(pbCoreElement);
        PBCoreElement pbCoreElementToReturn = null;
        while (!split.isEmpty()
                && (pbCoreElementToReturn == null || !pbCoreElementToReturn.getFullPath().equals(fullpath)
                        || index != pbCoreElementToReturn.getIndex())) {
            String remove = split.remove(0);
            List<PBCoreElement> collect = elementsToProcess
                    .stream()
                    .filter(pbce -> pbce.getName().equals(remove))
                    .collect(Collectors.toList());
            pbCoreElementToReturn = collect.isEmpty() ? null
                    : collect.stream().filter(pbce -> pbce.getIndex() == index).findFirst().orElse(null);

            if (pbCoreElementToReturn == null) {
                pbCoreElementToReturn = collect.isEmpty() ? null : collect.get(0);
            }
            if (pbCoreElementToReturn == null) {
                return null;
            }
            elementsToProcess = pbCoreElementToReturn.getOrderedSubElements();
        }
        return pbCoreElementToReturn;
    }

    private static PBCoreElement mapPBCoreElementToString(PBCoreElement pbCoreElement, String key,
                                                          List<CSVElementMapper> mappers) {

        PBCoreElement element = null;
        List<String> strings = mappers.stream().map(CSVElementMapper::getName).collect(Collectors.toList());
        String string = "";
        int index = -1;
        for (String s : strings) {
            if (key.startsWith(s)) {
                try {
                    index = Integer.parseInt(key.split(s)[1]);
                    string = s;
                    break;
                }
                catch (NumberFormatException e) {
                }
            }
        }
        String finalString = string;
        CSVElementMapper csvElementMapper = mappers
                .stream()
                .filter(em -> em.getName().equals(finalString))
                .findFirst()
                .orElse(null);
        if (csvElementMapper != null) {
            element = findElement(pbCoreElement, csvElementMapper.getElementFullPath(), index);
        }
        return element;
    }
}
