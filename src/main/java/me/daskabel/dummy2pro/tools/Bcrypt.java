package me.daskabel.dummy2pro.tools;

/**
 * Kleines Hilfswerkzeug zum manuellen Erzeugen und Prüfen von BCrypt-Hashes.
 *
 * Die Klasse ist unabhängig von der eigentlichen Anwendung und dient nur
 * lokalen Test- oder Pflegezwecken, etwa um schnell einen Hash für ein
 * Passwort zu erzeugen und die Verifikation direkt in der Konsole zu prüfen.
 */

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Damit man manuell Sachen encrypten kann. Z.B. zu Testzwecken
 */
public class Bcrypt
{
    public static void main(String[] args)
    {
        String raw = "ASDFqwer1234!";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // BCrypt erzeugt wegen Salt bei jedem Lauf einen neuen Hash.
        // Der Vergleich über matches(...) muss trotzdem true liefern.
        String hash = encoder.encode(raw);

        System.out.println("hash=" + hash);
        System.out.println("matches=" + encoder.matches(raw, hash));
    }
}
