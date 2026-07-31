package com.uteq.software.mlkit_sem14;

import com.google.mlkit.vision.barcode.common.Barcode;

/**
 * Utilidades comunes para presentar los codigos detectados por ML Kit.
 * Las usan tanto la deteccion sobre foto (MainActivity) como el escaneo
 * en vivo con CameraX (ScannerActivity).
 */
final class Codigos {

    private Codigos() {
    }

    /**
     * Texto legible del contenido. Para los tipos estructurados (URL, WiFi, contacto,
     * telefono, etc.) se muestran los campos que ML Kit ya separo; para el resto se
     * devuelve el valor tal cual viene codificado.
     */
    static String texto(Barcode codigo) {
        switch (codigo.getValueType()) {
            case Barcode.TYPE_URL:
                Barcode.UrlBookmark url = codigo.getUrl();
                if (url != null) return url.getTitle() + " " + url.getUrl();
                break;
            case Barcode.TYPE_WIFI:
                Barcode.WiFi wifi = codigo.getWifi();
                if (wifi != null) {
                    return "Red: " + wifi.getSsid() + "\nClave: " + wifi.getPassword();
                }
                break;
            case Barcode.TYPE_PHONE:
                Barcode.Phone tel = codigo.getPhone();
                if (tel != null) return tel.getNumber();
                break;
            case Barcode.TYPE_EMAIL:
                Barcode.Email correo = codigo.getEmail();
                if (correo != null) {
                    return correo.getAddress() + "\nAsunto: " + correo.getSubject();
                }
                break;
            case Barcode.TYPE_GEO:
                Barcode.GeoPoint geo = codigo.getGeoPoint();
                if (geo != null) return geo.getLat() + ", " + geo.getLng();
                break;
            case Barcode.TYPE_CONTACT_INFO:
                Barcode.ContactInfo contacto = codigo.getContactInfo();
                if (contacto != null && contacto.getName() != null) {
                    return contacto.getName().getFormattedName()
                            + "\nOrganizacion: " + contacto.getOrganization();
                }
                break;
        }

        String valor = codigo.getRawValue();
        return valor != null ? valor : "(no legible)";
    }

    static String nombreFormato(int formato) {
        switch (formato) {
            case Barcode.FORMAT_QR_CODE:      return "QR";
            case Barcode.FORMAT_AZTEC:        return "Aztec";
            case Barcode.FORMAT_DATA_MATRIX:  return "Data Matrix";
            case Barcode.FORMAT_PDF417:       return "PDF417";
            case Barcode.FORMAT_EAN_13:       return "EAN-13";
            case Barcode.FORMAT_EAN_8:        return "EAN-8";
            case Barcode.FORMAT_UPC_A:        return "UPC-A";
            case Barcode.FORMAT_UPC_E:        return "UPC-E";
            case Barcode.FORMAT_CODE_39:      return "Code 39";
            case Barcode.FORMAT_CODE_93:      return "Code 93";
            case Barcode.FORMAT_CODE_128:     return "Code 128";
            case Barcode.FORMAT_CODABAR:      return "Codabar";
            case Barcode.FORMAT_ITF:          return "ITF";
            default:                          return "Desconocido";
        }
    }

    static String nombreTipo(int tipo) {
        switch (tipo) {
            case Barcode.TYPE_URL:            return "URL";
            case Barcode.TYPE_WIFI:           return "WiFi";
            case Barcode.TYPE_TEXT:           return "Texto";
            case Barcode.TYPE_PHONE:          return "Telefono";
            case Barcode.TYPE_EMAIL:          return "Correo";
            case Barcode.TYPE_SMS:            return "SMS";
            case Barcode.TYPE_GEO:            return "Ubicacion";
            case Barcode.TYPE_CONTACT_INFO:   return "Contacto";
            case Barcode.TYPE_CALENDAR_EVENT: return "Evento";
            case Barcode.TYPE_DRIVER_LICENSE: return "Licencia";
            case Barcode.TYPE_ISBN:           return "ISBN";
            case Barcode.TYPE_PRODUCT:        return "Producto";
            default:                          return "Desconocido";
        }
    }
}
