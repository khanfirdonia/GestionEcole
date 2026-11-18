package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Classe utilitaire pour la gestion des dates
 */
public class DateUtils {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter MYSQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MYSQL_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MYSQL_DATETIME_MILLIS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    /**
     * Formater une LocalDate
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMAT);
    }

    /**
     * Formater une LocalDateTime
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * Formater l'heure
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMAT);
    }

    /**
     * Convertir une String (dd/MM/yyyy) en LocalDate
     */
    public static LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateString, MYSQL_DATE_FORMAT);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /**
     * Convertir une String (dd/MM/yyyy HH:mm:ss) en LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        try {
            return LocalDateTime.parse(dateTimeString, DATETIME_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeString, MYSQL_DATETIME_FORMAT);
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDateTime.parse(dateTimeString, MYSQL_DATETIME_MILLIS_FORMAT);
                } catch (DateTimeParseException ignoredAgain) {
                    try {
                        return LocalDateTime.parse(dateTimeString.replace(' ', 'T'));
                    } catch (Exception ignoredLast) {
                        return null;
                    }
                }
            }
        }
    }

    public static String formatStoredDate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        LocalDate date = parseDate(value.trim());
        return date != null ? formatDate(date) : value.trim();
    }

    public static String formatStoredDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        LocalDateTime dateTime = parseDateTime(value.trim());
        if (dateTime != null) {
            return formatDateTime(dateTime);
        }

        LocalDate date = parseDate(value.trim());
        return date != null ? formatDate(date) : value.trim();
    }

    public static String formatDateRange(String start, String end) {
        String formattedStart = formatStoredDate(start);
        String formattedEnd = formatStoredDate(end);

        if (formattedStart.isEmpty()) {
            return formattedEnd;
        }
        if (formattedEnd.isEmpty()) {
            return formattedStart;
        }
        return formattedStart + " - " + formattedEnd;
    }

    /**
     * Obtenir la date du jour
     */
    public static String getTodayAsString() {
        return formatDate(LocalDate.now());
    }

    /**
     * Obtenir la date et l'heure actuelle
     */
    public static String getNowAsString() {
        return formatDateTime(LocalDateTime.now());
    }

    /**
     * Calculer l'âge basé sur la date de naissance
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now());
    }

    /**
     * Calculer le nombre de jours entre deux dates
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Vérifier si une date est aujourd'hui
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }

    /**
     * Vérifier si une date est dans le passé
     */
    public static boolean isPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    /**
     * Vérifier si une date est dans le futur
     */
    public static boolean isFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }

    /**
     * Obtenir le premier jour du mois
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(1);
    }

    /**
     * Obtenir le dernier jour du mois
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(date.lengthOfMonth());
    }
}
