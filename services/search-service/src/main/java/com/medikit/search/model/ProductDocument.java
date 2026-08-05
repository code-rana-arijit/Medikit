package com.medikit.search.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document(indexName = "medikit-products")
public class ProductDocument {

    @Id
    private String productId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String saltComposition;

    @Field(type = FieldType.Keyword)
    private String manufacturer;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String pharmacyId;

    @Field(type = FieldType.Double)
    private BigDecimal sellingPrice;

    @Field(type = FieldType.Boolean)
    private boolean prescriptionRequired;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Text)
    private List<String> searchTokens = new ArrayList<>();

    public ProductDocument() {
    }

    public static ProductDocument from(SearchableProduct p) {
        ProductDocument d = new ProductDocument();
        d.setProductId(p.getProductId());
        d.setName(p.getName());
        d.setSaltComposition(p.getSaltComposition());
        d.setManufacturer(p.getManufacturer());
        d.setCategoryName(p.getCategoryName());
        d.setPharmacyId(p.getPharmacyId());
        d.setSellingPrice(p.getSellingPrice());
        d.setPrescriptionRequired(p.isPrescriptionRequired());
        d.setActive(p.isActive());
        d.setSearchTokens(p.getSearchTokens());
        return d;
    }

    public SearchableProduct toSearchable() {
        SearchableProduct p = new SearchableProduct();
        p.setProductId(productId);
        p.setName(name);
        p.setSaltComposition(saltComposition);
        p.setManufacturer(manufacturer);
        p.setCategoryName(categoryName);
        p.setPharmacyId(pharmacyId);
        p.setSellingPrice(sellingPrice);
        p.setPrescriptionRequired(prescriptionRequired);
        p.setActive(active);
        p.setSearchTokens(searchTokens);
        return p;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSaltComposition() {
        return saltComposition;
    }

    public void setSaltComposition(String saltComposition) {
        this.saltComposition = saltComposition;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public boolean isPrescriptionRequired() {
        return prescriptionRequired;
    }

    public void setPrescriptionRequired(boolean prescriptionRequired) {
        this.prescriptionRequired = prescriptionRequired;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getSearchTokens() {
        return searchTokens;
    }

    public void setSearchTokens(List<String> searchTokens) {
        this.searchTokens = searchTokens != null ? searchTokens : new ArrayList<>();
    }
}
