
package com.emc.storageos.vasa;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.0.3
 * 2015-10-01T14:11:45.149+05:30
 * Generated source version: 3.0.3
 */

@WebFault(name = "NotSupported", targetNamespace = "http://com.vmware.vim.vasa/2.0/xsd")
public class NotSupported_Exception extends java.lang.Exception {
    
    private com.emc.storageos.vasa.NotSupported2 notSupported;

    public NotSupported_Exception() {
        super();
    }
    
    public NotSupported_Exception(String message) {
        super(message);
    }
    
    public NotSupported_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupported_Exception(String message, com.emc.storageos.vasa.NotSupported2 notSupported) {
        super(message);
        this.notSupported = notSupported;
    }

    public NotSupported_Exception(String message, com.emc.storageos.vasa.NotSupported2 notSupported, Throwable cause) {
        super(message, cause);
        this.notSupported = notSupported;
    }

    public com.emc.storageos.vasa.NotSupported2 getFaultInfo() {
        return this.notSupported;
    }
}
