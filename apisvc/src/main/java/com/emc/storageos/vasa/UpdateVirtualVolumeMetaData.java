
package com.emc.storageos.vasa;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element name="vvolId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="keyValuePair" type="{http://data.vasa.vim.vmware.com/xsd}NameValuePair" maxOccurs="unbounded"/>
 *         &lt;element name="containerCookie" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "vvolId",
    "keyValuePair",
    "containerCookie"
})
@XmlRootElement(name = "updateVirtualVolumeMetaData")
public class UpdateVirtualVolumeMetaData {

    @XmlElement(required = true)
    protected String vvolId;
    @XmlElement(required = true)
    protected List<NameValuePair> keyValuePair;
    @XmlElementRef(name = "containerCookie", namespace = "http://com.vmware.vim.vasa/2.0/xsd", type = JAXBElement.class, required = false)
    protected JAXBElement<String> containerCookie;

    /**
     * Gets the value of the vvolId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVvolId() {
        return vvolId;
    }

    /**
     * Sets the value of the vvolId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVvolId(String value) {
        this.vvolId = value;
    }

    /**
     * Gets the value of the keyValuePair property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the keyValuePair property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKeyValuePair().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NameValuePair }
     * 
     * 
     */
    public List<NameValuePair> getKeyValuePair() {
        if (keyValuePair == null) {
            keyValuePair = new ArrayList<NameValuePair>();
        }
        return this.keyValuePair;
    }

    /**
     * Gets the value of the containerCookie property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getContainerCookie() {
        return containerCookie;
    }

    /**
     * Sets the value of the containerCookie property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setContainerCookie(JAXBElement<String> value) {
        this.containerCookie = value;
    }

}
