package com.medikit.search.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SearchableProduct {

    private String productId;
    private String name;
    private String saltComposition;
    private String manufacturer;
    private String categoryName;
    private String pharmacyId;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private boolean prescriptionRequired;
    private boolean active;
    private String imageUrl;
    private List<String> searchTokens = new ArrayList<>();

    public SearchableProduct() {
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

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getSearchTokens() {
        return searchTokens;
    }

    public void setSearchTokens(List<String> searchTokens) {
        this.searchTokens = searchTokens != null ? searchTokens : new ArrayList<>();
    }
}
