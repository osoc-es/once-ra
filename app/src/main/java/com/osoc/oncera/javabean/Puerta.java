package com.osoc.oncera.javabean;

import java.io.Serializable;

public class Puerta implements Serializable {

    private Float anchura;
    private Float altura;
    private String tipoPuerta;
    private Float alturaPomo;
    private String tipoMecanismo;
    private Boolean accesible;
    private String codCentro;
    private String id;


    public Puerta(Float anchura, Float altura, String tipoPuerta, Float alturaPomo, String tipoMecanismo, Boolean accesible, String codCentro, String id) {
        this.anchura = anchura;
        this.altura = altura;
        this.tipoPuerta = tipoPuerta;
        this.alturaPomo = alturaPomo;
        this.tipoMecanismo = tipoMecanismo;
        this.accesible = accesible;
        this.codCentro = codCentro;
        this.id = id;
    }

    public Puerta() {
    }

    public Float getAnchura() {
        return anchura;
    }

    public void setAnchura(Float anchura) {
        this.anchura = anchura;
    }

    public Float getAltura() {
        return altura;
    }

    public void setAltura(Float altura) {
        this.altura = altura;
    }

    public String getTipoPuerta() {
        return tipoPuerta;
    }

    public void setTipoPuerta(String tipoPuerta) {
        this.tipoPuerta = tipoPuerta;
    }

    public Float getAlturaPomo() {
        return alturaPomo;
    }

    public void setAlturaPomo(Float alturaPomo) {
        this.alturaPomo = alturaPomo;
    }

    public String getTipoMecanismo() {
        return tipoMecanismo;
    }

    public void setTipoMecanismo(String tipoMecanismo) {
        tipoMecanismo = tipoMecanismo;
    }

    public Boolean getAccesible() {
        return accesible;
    }

    public void setAccesible(Boolean accesible) {
        this.accesible = accesible;
    }

    public String getCodCentro() {
        return codCentro;
    }

    public void setCodCentro(String codCentro) {
        this.codCentro = codCentro;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

