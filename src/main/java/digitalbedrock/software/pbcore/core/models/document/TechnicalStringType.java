package digitalbedrock.software.pbcore.core.models.document;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Definition: The technicalStringType schema type adds the attributes of unitsOfMeasure and annotation.
 *
 * <p>
 * Java class for technicalStringType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="technicalStringType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attGroup ref="{http://www.pbcore.org/PBCore/PBCoreNamespace.html}sourceVersionGroup"/>
 *       &lt;attribute name="unitsOfMeasure" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@SuppressWarnings("WeakerAccess")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "technicalStringType", namespace = "http://www.pbcore.org/PBCore/PBCoreNamespace.html", propOrder = {
        "value" })
public class TechnicalStringType {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "unitsOfMeasure")
    protected String unitsOfMeasure;
    @XmlAttribute(name = "source")
    protected String source;
    @XmlAttribute(name = "ref")
    protected String ref;
    @XmlAttribute(name = "version")
    protected String version;
    @XmlAttribute(name = "annotation")
    protected String annotation;

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getValue() {

        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setValue(String value) {

        this.value = value;
    }

    /**
     * Gets the value of the unitsOfMeasure property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getUnitsOfMeasure() {

        return unitsOfMeasure;
    }

    /**
     * Sets the value of the unitsOfMeasure property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setUnitsOfMeasure(String value) {

        this.unitsOfMeasure = value;
    }

    /**
     * Gets the value of the source property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getSource() {

        return source;
    }

    /**
     * Sets the value of the source property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setSource(String value) {

        this.source = value;
    }

    /**
     * Gets the value of the ref property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getRef() {

        return ref;
    }

    /**
     * Sets the value of the ref property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setRef(String value) {

        this.ref = value;
    }

    /**
     * Gets the value of the version property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getVersion() {

        return version;
    }

    /**
     * Sets the value of the version property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setVersion(String value) {

        this.version = value;
    }

    /**
     * Gets the value of the annotation property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getAnnotation() {

        return annotation;
    }

    /**
     * Sets the value of the annotation property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setAnnotation(String value) {

        this.annotation = value;
    }

}
