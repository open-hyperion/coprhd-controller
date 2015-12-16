
package com.emc.storageos.vasa;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.0.3
 * 2015-10-01T14:11:45.203+05:30
 * Generated source version: 3.0.3
 */

@WebFault(name = "InvalidProfile", targetNamespace = "http://com.vmware.vim.vasa/2.0/xsd")
public class InvalidProfile_Exception extends java.lang.Exception {
    
    private com.emc.storageos.vasa.InvalidProfile2 invalidProfile;

    public InvalidProfile_Exception() {
        super();
    }
    
    public InvalidProfile_Exception(String message) {
        super(message);
    }
    
    public InvalidProfile_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProfile_Exception(String message, com.emc.storageos.vasa.InvalidProfile2 invalidProfile) {
        super(message);
        this.invalidProfile = invalidProfile;
    }

    public InvalidProfile_Exception(String message, com.emc.storageos.vasa.InvalidProfile2 invalidProfile, Throwable cause) {
        super(message, cause);
        this.invalidProfile = invalidProfile;
    }

    public com.emc.storageos.vasa.InvalidProfile2 getFaultInfo() {
        return this.invalidProfile;
    }
}
