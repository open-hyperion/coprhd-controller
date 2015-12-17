/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.hpux.command;

import org.apache.commons.lang.StringUtils;

import com.iwave.ext.command.CommandException;

public class InsfRescanDevicesCommand extends HpuxCommand {

    public InsfRescanDevicesCommand() {
        setCommand("insf -C disk");
        setRunAsRoot(true);
    }

    @Override
    protected void processError() throws CommandException {
        String stdout = StringUtils.trimToEmpty(getOutput().getStdout());
        String stderr = StringUtils.trimToEmpty(getOutput().getStderr());
        if (stdout.isEmpty() || !stderr.isEmpty()) {
            super.processError();
        }
    }

}
