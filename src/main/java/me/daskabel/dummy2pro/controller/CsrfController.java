package me.daskabel.dummy2pro.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stellt das aktuelle CSRF-Token für das Frontend bereit.
 *
 * Das Frontend kann das Token über diesen Endpunkt laden und bei
 * schreibenden Requests mitsenden.
 */
@RestController
public class CsrfController
{
    /**
     * Liefert das aktuelle CSRF-Token zurück.
     *
     * @param csrfToken von Spring bereitgestelltes Token
     * @return aktuelles CSRF-Token
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken)
    {
        return csrfToken;
    }
}
