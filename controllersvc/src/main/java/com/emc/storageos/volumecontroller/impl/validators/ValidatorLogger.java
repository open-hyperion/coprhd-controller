package com.emc.storageos.volumecontroller.impl.validators;

import org.slf4j.Logger;

/**
 * Logger for validations.
 */
public class ValidatorLogger {
    private Logger log;
    private StringBuilder msgs = new StringBuilder();

    public ValidatorLogger() {
    }

    /**
     * Log a discrepency in the data.
     * @param id -- Identity of the domain object
     * @param field -- Field with discrepency
     * @param db -- Database value
     * @param hw -- Hardware value
     */
    public void logDiff(String id, String field, String db, String hw) {
        String msg = String.format("id: %s field: %s db: %s hw: %s", id, field, db, hw);
        msgs.append(msg + "\n");
        log.info(msg);
    } 
    
    public ValidatorLogger(Logger log) {
        this.log = log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public StringBuilder getMsgs() {
        return msgs;
    }
    
    public boolean hasErrors() {
        return msgs.length() > 0;
    }
}