
package com.emc.storageos.vasa;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.0.3
 * 2015-10-01T14:11:45.190+05:30
 * Generated source version: 3.0.3
 */

@WebFault(name = "ActivateProviderFailed", targetNamespace = "http://com.vmware.vim.vasa/2.0/xsd")
public class ActivateProviderFailed_Exception extends java.lang.Exception {
    
    private com.emc.storageos.vasa.ActivateProviderFailed2 activateProviderFailed;

    public ActivateProviderFailed_Exception() {
        super();
    }
    
    public ActivateProviderFailed_Exception(String message) {
        super(message);
    }
    
    public ActivateProviderFailed_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public ActivateProviderFailed_Exception(String message, com.emc.storageos.vasa.ActivateProviderFailed2 activateProviderFailed) {
        super(message);
        this.activateProviderFailed = activateProviderFailed;
    }

    public ActivateProviderFailed_Exception(String message, com.emc.storageos.vasa.ActivateProviderFailed2 activateProviderFailed, Throwable cause) {
        super(message, cause);
        this.activateProviderFailed = activateProviderFailed;
    }

    public com.emc.storageos.vasa.ActivateProviderFailed2 getFaultInfo() {
        return this.activateProviderFailed;
    }
}
