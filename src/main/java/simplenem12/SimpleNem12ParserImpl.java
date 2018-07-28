package simplenem12;

import java.io.*;
import java.math.BigDecimal;

import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

    private static final String BEGINNING_ENTRY = "100";
    private static final String ENDING_ENTRY = "900";
    private static final String START_BLOCK_ENTRY = "200";
    private static final String START_BLOCK_DATE_ENTRY = "300";
    private static final String VALUE_SEPARATOR = ",";
    private static final String EMPTY_STRING = "";
    private static final int NMI_LENGTH = 10;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {

        ArrayList<MeterRead> meterReadForNMIs = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(simpleNem12File.toPath());

            lines.removeIf(item -> EMPTY_STRING.equals(item.trim()));

            if (!validateBeginAndEndString(lines)) {
                System.out.println("Missing Start and End tag entries");
                return meterReadForNMIs;
            }

            for (int lineNumber = 0; lineNumber < lines.size(); ) {
                String line = lines.get(lineNumber);
                if (START_BLOCK_ENTRY.equals(line.split(VALUE_SEPARATOR)[0])) {
                    String meterNmi = line.split(VALUE_SEPARATOR)[1];
                    if (meterNmi.length() != NMI_LENGTH) {
                        System.out.println("ignoring the NMI for invalid length " + meterNmi);
                        lineNumber++;
                        continue;
                    }
                    MeterRead meterRead = new MeterRead(meterNmi, EnergyUnit.KWH);
                    lineNumber++;
                    SortedMap<LocalDate, MeterVolume> volumes = new TreeMap<>();
                    while (START_BLOCK_DATE_ENTRY.equals(lines.get(lineNumber).split(VALUE_SEPARATOR)[0])) {
                        String lineVal = lines.get(lineNumber);
                        LocalDate date = parseDate(lineVal);
                        MeterVolume meterVolume = getMeterVolume(lines.get(lineNumber));
                        if (date != null && meterVolume != null) {
                            volumes.put(date, meterVolume);
                        }
                        lineNumber++;
                    }
                    meterRead.setVolumes(volumes);
                    meterReadForNMIs.add(meterRead);
                } else {
                    lineNumber++;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return meterReadForNMIs;
    }

    private LocalDate parseDate(String lineVal) {
        try {
            return LocalDate.parse(lineVal.split(VALUE_SEPARATOR)[1], formatter);
        } catch (DateTimeParseException dtpe) {
            System.out.println("ignoring the entry missing Date :"+lineVal);
            return null;
        }
    }

    private MeterVolume getMeterVolume(String lineVal) {
        try {
            BigDecimal volume = new BigDecimal(lineVal.split(VALUE_SEPARATOR)[2]);
            String qualityVal = lineVal.split(VALUE_SEPARATOR)[3];
            return new MeterVolume(volume,
                    getMeterQuality(qualityVal));
        } catch (IllegalArgumentException iae) {
            System.out.println("ignoring the entry missing Volume/Quality :"+lineVal);
            return null;
        }
    }

    private Quality getMeterQuality(String qualityVal) throws IllegalArgumentException {
            return Quality.valueOf(qualityVal);
    }

    private boolean validateBeginAndEndString(List<String> lines) {
        return lines.get(0).equals(BEGINNING_ENTRY)
                && lines.get(lines.size() - 1).equals(ENDING_ENTRY);
    }
}

