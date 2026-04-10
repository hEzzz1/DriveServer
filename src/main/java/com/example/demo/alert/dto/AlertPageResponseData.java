package com.example.demo.alert.dto;

import java.util.List;

public class AlertPageResponseData {

    private long total;
    private int page;
    private int size;
    private List<AlertListItemData> items;

    public AlertPageResponseData(long total, int page, int size, List<AlertListItemData> items) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<AlertListItemData> getItems() {
        return items;
    }

    public void setItems(List<AlertListItemData> items) {
        this.items = items;
    }
}
