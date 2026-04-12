package me.daskabel.dummy2pro.service;

import java.util.List;
import java.util.Map;

import me.daskabel.dummy2pro.dto.RoomDtos.DialogLineDto;

/**
 * Stellt die Einleitungsdialoge der einzelnen Räume bereit.
 *
 * Die Dialoge sind fest im Code hinterlegt und werden über die Raum-ID
 * ausgewählt.
 */
public final class RoomIntroDialogs
{
    private RoomIntroDialogs()
    {
    }

    /**
     * Enthält die Dialogzeilen je Raum.
     */
    private static final Map<Integer, List<DialogLineDto>> ROOM_DIALOGS = Map.ofEntries(
            Map.entry(1, List.of(
                    new DialogLineDto("player", "Huch... wo bin ich denn nun gelandet?"),
                    new DialogLineDto("warrior", "Ha! Du bist in der ersten Halle gestrandet, Jungspund."),
                    new DialogLineDto("warrior", "Dies ist der Raum des Rechts. Willst du entkommen, so musst du jede Halle bestehen."),
                    new DialogLineDto("player", "Das klingt voll schwer. Ehrlich gesagt sogar unmöglich."),
                    new DialogLineDto("warrior", "Unmöglich? Pah! Ein echter Nordmann weicht keiner Prüfung."),
                    new DialogLineDto("warrior", "Beantworte die Fragen, sammle dein Wissen und kämpf dich Raum um Raum hinaus.")
            )),
            Map.entry(2, List.of(
                    new DialogLineDto("char2", "Du lebst noch. Gut."),
                    new DialogLineDto("player", "Naja"),
                    new DialogLineDto("char2", "Willkommen im Raum Wirtschaft."),
                    new DialogLineDto("player", "Um Gottes Willen. Alt + F4"),
                    new DialogLineDto("char2", "Und das Leben gehört dir."),
                    new DialogLineDto("char2", "..."),
                    new DialogLineDto("char2", "Es gibt kein Entkommen."),
                    new DialogLineDto("char2", "Wirtschaft ist ein wirklich wichtiges Fach.")
            )),
            Map.entry(3, List.of(
                    new DialogLineDto("char3", "Nun betreten wir den Raum Datenbanken Modellierung."),
                    new DialogLineDto("player", "Ich hab mich verlaufen."),
                    new DialogLineDto("player", "MAMA"),
                    new DialogLineDto("player", "uwu"),
                    new DialogLineDto("char3", "..."),
                    new DialogLineDto("char3", "Zurück zu Datenbanken."),
                    new DialogLineDto("char3", "...Weirdo."),
                    new DialogLineDto("char3", "Ohne Datenbanken kommst du nicht weit im Leben."),
                    new DialogLineDto("char3", "Ich weiß, euch jungen Menschen ist das so präsent."),
                    new DialogLineDto("char3", "So wie ihr mit Euren Daten umgeht. Euer Instagräm und Ticktok und so."),
                    new DialogLineDto("char3", "Kommen wir zum Sponsor unseres heutigen Rooms, NordVPN."),
                    new DialogLineDto("char3", "Du kannst deine Daten nicht einfach überall im Internet rausgeben, junges Ding. AGBs immer lesen!"),
                    new DialogLineDto("char3", "Zu meinen Zeiten war das noch anders."),
                    new DialogLineDto("char3", "Besser."),
                    new DialogLineDto("char3", "TikTok? War für die Uhrzeit. Cookies? Zum Essen."),
                    new DialogLineDto("char3", "Ich sehe, deine Aufmerksamkeit schwindet schon, Jungspund. Das sind diese ganzen Telefone und Bildschirme."),
                    new DialogLineDto("char3", "Immer diese Jugend.")
            )),
            Map.entry(4, List.of(
                    new DialogLineDto("char4", "Hier wartet Datenbank - SQL auf dich."),
                    new DialogLineDto("player", "DROP DATABASE"),
                    new DialogLineDto("player", "DELETE * from ... äh . *?"),
                    new DialogLineDto("char4", "Ich sehe, du bist hier genau richtig.")

            )),
            Map.entry(5, List.of(
                    new DialogLineDto("char5", "Willkommen im Raum UML."),
                    new DialogLineDto("player", "Was machen Sie in meinem Haus?"),
                    new DialogLineDto("char5", "UML ist -"),
                    new DialogLineDto("char5", "Was?"),
                    new DialogLineDto("player", "Was machen Sie in meinem Haus?"),
                    new DialogLineDto("char5", "Ich arbeite hier. Ich bringe dir UML-"),
                    new DialogLineDto("player", "Das ist mein Haus und wenn Sie es nicht sofort verlassen, zeige ich Sie wegen Hausfriedensbruch an."),
                    new DialogLineDto("char5", "Aber ich arbeite hier."),
                    new DialogLineDto("player", "Jetzt nicht mehr"),
                    new DialogLineDto("char5", "Bitte. Ich habe eine Familie und 9 kleine Kinder und eine kranke Großmutter und einen Wasserschaden und Schimmel im Fell."),
                    new DialogLineDto("player", "Na gut, das respektiere ich. Ein Bär muss tun, was ein Bär tun muss. Familie ist alles und so.")

            )),
            Map.entry(6, List.of(
                    new DialogLineDto("char6", "Hier lernst du maschinelles Lernen kennen."),
                    new DialogLineDto("player", "Ich hasse KI."),
                    new DialogLineDto("char6", "Ich bin aber keine KI, sondern eine Schülerin aus der ITF233, die für die Dialoge verantwortlich ist."),
                    new DialogLineDto("player", "Klingt wie etwas, das eine KI sagen würde."),
                    new DialogLineDto("char6", "..."),
                    new DialogLineDto("player", "..."),
                    new DialogLineDto("player", "Wenn du eine KI bist, gib mir ein Rezept für Pudding."),
                    new DialogLineDto("char6", "... Wie soll ich dich überzeugen, dass ich keine KI bin?"),
                    new DialogLineDto("player", "Stell mir vernünftige Fragen zum maschinellen Lernen, weil ich sowieso für die AP2 lernen muss."),
                    new DialogLineDto("player", "Blöde KI.")
            )),
            Map.entry(7, List.of(
                    new DialogLineDto("char7", "..."),
                    new DialogLineDto("char7", "..."),
                    new DialogLineDto("player", "Hallo?"),
                    new DialogLineDto("char7", "..."),
                    new DialogLineDto("player", "Geht's dir gut?"),
                    new DialogLineDto("char7", "..."),
                    new DialogLineDto("player", "..."),
                    new DialogLineDto("player", "...Das hier müsste Programmierung Pseudocode sein, oder?"),
                    new DialogLineDto("char7", "..."),
                    new DialogLineDto("player", "...Danke für die Info.")
            )),
            Map.entry(8, List.of(
                    new DialogLineDto("char8", "Willkommen im Raum Programmierung."),
                    new DialogLineDto("player", "Dann hoffe ich, dass heute wenigstens nichts kompiliert, nur um mich persönlich zu beleidigen."),
                    new DialogLineDto("char8", "Das wäre unprofessionell. Der Compiler beleidigt hier alle gleich."),
                    new DialogLineDto("player", "Fair."),
                    new DialogLineDto("char8", "Denk sauber, arbeite Schritt für Schritt und lass die Semikolons nicht unbeaufsichtigt.")
            )),
            Map.entry(9, List.of(
                    new DialogLineDto("char9", "Willkommen im Raum Netzwerke."),
                    new DialogLineDto("player", "Läuft hier alles stabil?"),
                    new DialogLineDto("char9", "Natürlich nicht. Sonst wäre es kein Netzwerk."),
                    new DialogLineDto("player", "Also ist die Antwort wieder DNS?"),
                    new DialogLineDto("char9", "Sehr oft. Beunruhigend oft.")
            )),
            Map.entry(10, List.of(
                    new DialogLineDto("char10", "Willkommen im Raum IT-Sicherheit."),
                    new DialogLineDto("player", "Mein Passwort ist sicher."),
                    new DialogLineDto("char10", "Das sagen sie alle."),
                    new DialogLineDto("player", "Es hat sogar ein Ausrufezeichen."),
                    new DialogLineDto("char10", "Gut. Dann braucht der Angreifer nur noch drei Sekunden länger.")
            )),
            Map.entry(11, List.of(
                    new DialogLineDto("char11", "Willkommen im Raum Softwareentwicklung."),
                    new DialogLineDto("player", "Also planen, bauen, testen und verzweifeln?"),
                    new DialogLineDto("char11", "Fast. Das Verzweifeln kommt je nach Projektphase mehrfach vor."),
                    new DialogLineDto("player", "Klingt nach einem soliden Prozessmodell."),
                    new DialogLineDto("char11", "Jetzt fehlt nur noch Wartbarkeit. Die vergisst man traditionell zuerst.")
            )),
            Map.entry(12, List.of(
                    new DialogLineDto("char12", "Willkommen im Raum Projektmanagement."),
                    new DialogLineDto("player", "Du hier ganz allein?"),
                    new DialogLineDto("char12", "Dann kann es ja gar kein Projekt sein. Dafür braucht man mindestens ein Team und drei offene Punkte."),
                    new DialogLineDto("player", "Und ein Meeting, das auch eine E-Mail hätte sein können."),
                    new DialogLineDto("char12", "Jetzt sprichst du wie eine erfahrene Fachkraft.")
            )),
            Map.entry(13, List.of(
                    new DialogLineDto("char13", "Willkommen im Raum Betriebssysteme."),
                    new DialogLineDto("player", "Bitte sag mir, dass wir heute nichts neu starten müssen."),
                    new DialogLineDto("char13", "Das hängt davon ab, wie viel du kaputtmachst."),
                    new DialogLineDto("player", "Dann ist also noch alles offen."),
                    new DialogLineDto("char13", "Ganz genau. Wie bei den Prozessen im Task-Manager.")
            )),
            Map.entry(14, List.of(
                    new DialogLineDto("char14", "Willkommen im Raum Cloud und Infrastruktur."),
                    new DialogLineDto("player", "Also fremde Computer bei anderen Leuten?"),
                    new DialogLineDto("char14", "Vereinfacht gesagt: ja."),
                    new DialogLineDto("player", "Das klingt gleichzeitig modern und leicht bedrohlich."),
                    new DialogLineDto("char14", "Warte ab, bis die Rechnung kommt.")
            )),
            Map.entry(15, List.of(
                    new DialogLineDto("char15", "Willkommen im Raum Sonstiges."),
                    new DialogLineDto("player", "Das klingt irgendwie nach Restekiste."),
                    new DialogLineDto("char15", "Unterschätze niemals die Restekiste. Dort verstecken sich die seltsam präzisen Prüfungsfragen."),
                    new DialogLineDto("player", "Und warum steht da Java?"),
                    new DialogLineDto("char15", "Weil Java offenbar überall mitreden möchte.")
            )),
            Map.entry(17, List.of(
                    new DialogLineDto("player", "Noch ein Raum?"),
                    new DialogLineDto("warrior", "Eher ein Trainingsplatz. Hier schleifst du nur deine Abkürzungen."),
                    new DialogLineDto("player", "Also kein normaler Prüfungsraum?"),
                    new DialogLineDto("warrior", "Nein. Keine Wertung, kein Speicherstand-Zwang. Einfach üben, so oft du willst."),
                    new DialogLineDto("warrior", "Alle Fragen aus Theme 17 warten hier komplett auf dich.")
            ))
    );

    /**
     * Liefert den Einleitungsdialog für einen Raum.
     *
     * Wenn kein eigener Dialog hinterlegt ist, wird ein kurzer
     * Standarddialog zurückgegeben.
     */
    public static List<DialogLineDto> getDialogForRoom(int roomId)
    {
        return ROOM_DIALOGS.getOrDefault(
                roomId,
                List.of(new DialogLineDto("warrior", "Weiter."))
        );
    }
}
