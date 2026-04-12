package me.daskabel.dummy2pro.security;

import java.io.Serializable;

/**
 * Repräsentiert den angemeldeten Benutzer innerhalb des
 * Sicherheitskontexts.
 *
 * Gespeichert werden die Benutzer-ID und der Benutzername.
 */
public record AuthenticatedUser(Long userId, String username) implements Serializable
{
}
