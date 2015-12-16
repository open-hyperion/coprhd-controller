
package com.emc.storageos.vasa;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ResourceInUse" type="{http://fault.vasa.vim.vmware.com/xsd}ResourceInUse" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "resourceInUse"
})
@XmlRootElement(name = "ResourceInUse")
public class ResourceInUse2 {

    @XmlElementRef(name = "ResourceInUse", namespace = "http://com.vmware.vim.vasa/2.0/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<ResourceInUse> resourceInUse;

    /**
     * Gets the value of the resourceInUse property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ResourceInUse }{@code >}
     *     
     */
    public JAXBElement<ResourceInUse> getResourceInUse() {
        return resourceInUse;
    }

    /**
     * Sets the value of the resourceInUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ResourceInUse }{@code >}
     *     
     */
    public void setResourceInUse(JAXBElement<ResourceInUse> value) {
        this.resourceInUse = value;
    }

}
