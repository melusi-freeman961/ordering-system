package com.kasigrill.ordering_system.menuitem;

public class MenuItemResponse {
    public String title;
    public  String desc;
    boolean saved;

    public MenuItemResponse(String title, String desc,boolean saved) {
        this.title = title;
        this.desc = desc;
        this.saved=saved;
    }
}
