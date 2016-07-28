package com.emc.storageos.systemservices.impl.logsvc.parse;

import com.emc.storageos.systemservices.impl.logsvc.LogMessage;
import com.emc.vipr.model.sys.logging.LogRequest;
import com.emc.vipr.model.sys.logging.LogSeverity;

/**
 * Created by sonalisahu on 7/27/16.
 */
public class LogOEParser extends LogParser {
    private final int TIME_LENGTH = 21;

    /**
     * Parse line from file to LogMessage If line does not match log format(it
     * is the message part for multiple lines log), return
     * LogMessage.CONTINUATION_LOGMESSAGE; if line matches log formant, time is
     * too late for time filter, return LogMessage.REJECTED_LAST_LOGMESSAGE if
     * line matches log formant, but does not match all filters, return
     * LogMessage.REJECTED_LOGMESSAGE; if line matches log formant and all
     * filters, return LogMessage object
     */
    @Override
    public LogMessage parseLine(String line, LogRequest info) {
        // length of the time 2013-11-20 13:56:48 [
        int lineLength = line.length();
        if (lineLength <= TIME_LENGTH || line.charAt(4) != '-'
                || line.charAt(7) != '-' || line.charAt(10) != ' '
                || line.charAt(13) != ':' || line.charAt(16) != ':'
                || line.charAt(19) != ',') {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }
        String yearStr = line.substring(0, 4);
        int year = toNumber(yearStr);
        if (year < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }
        String monthStr = line.substring(5, 7);
        int month = toNumber(monthStr);
        if (month < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }
        String dayStr = line.substring(8, 10);
        int day = toNumber(dayStr);
        if (day < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }
        String hourStr = line.substring(11, 13);
        int hour = toNumber(hourStr);
        if (hour < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }
        String minStr = line.substring(14, 16);
        int min = toNumber(minStr);
        if (min < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }
        String secStr = line.substring(17, 19);
        int sec = toNumber(secStr);
        if (sec < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }

        // test time filter
        int inTime = inTimeRange(year, month, day, hour, min, sec, 0, info);
        if (inTime < 0) { // too early
            return LogMessage.REJECTED_LOGMESSAGE;
        } else if (inTime > 0) { // too late
            return LogMessage.REJECTED_LAST_LOGMESSAGE;
        }

        final int level = LogSeverity.toLevel("DEBUG");
        if (level < 0) {
            return LogMessage.CONTINUATION_LOGMESSAGE;
        }

        LogMessage log = new LogMessage(getTime(year, month, day, hour, min, sec, 0),
                line.getBytes());

        log.setLevel(level);
	String[] splitLine = line.split(" ");
        int logOffset = line.indexOf(splitLine[2]);
        log.setLogOffset(logOffset);
	log.setTimeBytes(0, TIME_LENGTH - 2);
        return log;

    }
}

