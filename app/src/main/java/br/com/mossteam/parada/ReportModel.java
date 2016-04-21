package br.com.mossteam.parada;

import java.util.Date;

/**
 * Created by willian on 1/1/16.
 */
public class ReportModel {

    private String code = null;
    private String plate = null;
    private Date date = null;

    public ReportModel() { }

    public ReportModel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void save() { }
}
