package com.livemore.api.web.dto;

import java.util.ArrayList;
import java.util.List;

public class ExtractedDataDto {

    private String text = "";
    private List<String> foods = new ArrayList<>();
    private List<String> drinks = new ArrayList<>();
    private WorkDataDto work = new WorkDataDto();
    private List<String> symptoms = new ArrayList<>();
    private List<SymptomEventDto> events = new ArrayList<>();
    private List<String> triggers = new ArrayList<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getFoods() {
        return foods;
    }

    public void setFoods(List<String> foods) {
        this.foods = foods;
    }

    public List<String> getDrinks() {
        return drinks;
    }

    public void setDrinks(List<String> drinks) {
        this.drinks = drinks;
    }

    public WorkDataDto getWork() {
        return work;
    }

    public void setWork(WorkDataDto work) {
        this.work = work;
    }

    public List<String> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<String> symptoms) {
        this.symptoms = symptoms;
    }

    public List<SymptomEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<SymptomEventDto> events) {
        this.events = events;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<String> triggers) {
        this.triggers = triggers;
    }
}
