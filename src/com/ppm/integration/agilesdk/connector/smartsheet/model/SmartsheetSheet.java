package com.ppm.integration.agilesdk.connector.smartsheet.model;

import com.hp.ppm.user.model.User;
import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SmartsheetSheet extends SmartsheetObject {

    private final static Logger logger = LogManager.getLogger(SmartsheetSheet.class);


    // yyyy-MM-dd date
    private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final static SimpleDateFormat localDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final static DateTimeFormatter longDateTimeFormatter = new DateTimeFormatterBuilder()
            // date/time
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            // offset (hh:mm - "+00:00" when it's zero)
            .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
            // offset (hhmm - "+0000" when it's zero)
            .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
            // offset (hh - "Z" when it's zero)
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            // create formatter
            .toFormatter();


    public String accessLevel;
    public String permalink;
    public String path; // Workspace / Folder hierarchy this sheet is sitting in, or "Home" if available in home.

    public String getFullName() {
        return path == null ? name : (path + name);
    }

    public SmartsheetColumn[] columns;

    public SmartsheetRow[] rows;

    public class SmartsheetColumn extends SmartsheetObject {
        public Integer index;
        public String title;
        public String type;
        public String [] tags;
        public String [] options;
    }

    public class SmartsheetRow extends SmartsheetObject {
        public Integer rowNumber;
        public String parentId;
        public SmartsheetCell[] cells;

        public boolean isBlank() {
            return cells == null || cells.length == 0 || Arrays.stream(cells).allMatch(cell -> cell.value == null);
        }

        public class SmartsheetCell {
            public String columnId;
            public String value;
            public String displayValue;

            public Date getValueAsDate() {
                return parseDate(value);
            }

            private Date parseDate(String dateStr) {
                if (StringUtils.isBlank(dateStr)) {
                    return null;
                }

                try {
                    if (dateStr.contains("T")) {
                        try {
                            ZonedDateTime date = ZonedDateTime.parse(dateStr, longDateTimeFormatter);
                            return Date.from(date.toInstant());
                        } catch (DateTimeParseException de) {
                            // ABSTRACT_DATETIME field types have format YYYY-MM-ddTHH:mm:ss without any timezone info.
                            return localDateTimeFormat.parse(dateStr);
                        }
                    } else {
                        // Format yyyy-MM-dd
                        return shortDateFormat.parse(dateStr);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse Date string " + dateStr + " , ignoring date.", e);
                    return null;
                }
            }

            public List<Long> getPeoplesValue(UserProvider userProvider) {
                List<Long> ppmResourceIds = new ArrayList<>();

                String emailStr = this.value;

                if (emailStr == null || !emailStr.contains("@")) {
                    return ppmResourceIds;
                }

                Long userId = getResourceIdFromEmailOrUsername(emailStr, userProvider);

                if (userId != null) {
                    ppmResourceIds.add(userId);
                }

                return ppmResourceIds;
            }

            private Long getResourceIdFromEmailOrUsername(String emailOrUsername, UserProvider userProvider) {
                if (StringUtils.isBlank(emailOrUsername)) {
                    return null;
                }
                User user = userProvider.getByEmail(emailOrUsername.trim());

                if (user == null) {
                    user = userProvider.getByUsername(emailOrUsername.trim());
                }

                if (user == null) {
                    return null;
                } else {
                    return user.getUserId();
                }
            }
        }

    }

}

