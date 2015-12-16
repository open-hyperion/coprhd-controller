
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
 *         &lt;element name="InvalidStatisticsContext" type="{http://fault.vasa.vim.vmware.com/xsd}InvalidStatisticsContext" minOccurs="0"/>
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
    "invalidStatisticsContext"
})
@XmlRootElement(name = "InvalidStatisticsContext")
public class InvalidStatisticsContext2 {

    @XmlElementRef(name = "InvalidStatisticsContext", namespace = "http://com.vmware.vim.vasa/2.0/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<InvalidStatisticsContext> invalidStatisticsContext;

    /**
     * Gets the value of the invalidStatisticsContext property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link InvalidStatisticsContext }{@code >}
     *     
     */
    public JAXBElement<InvalidStatisticsContext> getInvalidStatisticsContext() {
        return invalidStatisticsContext;
    }

    /**
     * Sets the value of the invalidStatisticsContext property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link InvalidStatisticsContext }{@code >}
     *     
     */
    public void setInvalidStatisticsContext(JAXBElement<InvalidStatisticsContext> value) {
        this.invalidStatisticsContext = value;
    }

}
