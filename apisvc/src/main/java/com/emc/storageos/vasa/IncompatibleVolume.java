
package com.emc.storageos.vasa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IncompatibleVolume complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IncompatibleVolume">
 *   &lt;complexContent>
 *     &lt;extension base="{http://com.vmware.vim.vasa/2.0/xsd}Exception">
 *       &lt;sequence>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IncompatibleVolume", namespace = "http://fault.vasa.vim.vmware.com/xsd")
public class IncompatibleVolume
    extends Exception
{


}
