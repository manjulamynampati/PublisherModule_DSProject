package com.package2.publisher.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PublisherModel {
    @Id
    String publisherId;
    String status;
    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
