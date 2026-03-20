package me.daskabel.dummy2pro.service;

import java.util.List;
import java.util.Map;

import me.daskabel.dummy2pro.dto.RoomDtos.DialogLineDto;

public final class RoomIntroDialogs
{
    private RoomIntroDialogs()
    {
    }

    private static final Map<Integer, List<DialogLineDto>> ROOM_DIALOGS = Map.of(
            1, List.of(
                    new DialogLineDto("player", "Huch... wo bin ich denn nun gelandet?"),
                    new DialogLineDto("warrior", "Ha! Du bist in der ersten Halle gestrandet, Jungspund."),
                    new DialogLineDto("warrior", "Dies ist der Raum des Rechts. Willst du entkommen, so musst du jede Halle bestehen."),
                    new DialogLineDto("player", "Das klingt voll schwer. Ehrlich gesagt sogar unmöglich."),
                    new DialogLineDto("warrior", "Unmöglich? Pah! Ein echter Nordmann weicht keiner Prüfung."),
                    new DialogLineDto("warrior", "Beantworte die Fragen, sammle dein Wissen und kämpf dich Raum um Raum hinaus.")
            ),
            2, List.of(
                    new DialogLineDto("char2", "Du lebst noch. Gut."),
                    new DialogLineDto("player", "Naja"),
                    new DialogLineDto("char2", "Willkommen im Raum Wirtschaft."),
                    new DialogLineDto("player", "Um Gottes Willen. Alt + F4"),
                    new DialogLineDto("char2", "Und das Leben gehört dir."),
                    new DialogLineDto("char2", "..."),
                    new DialogLineDto("char2", "Es gibt kein Entkommen."),
                    new DialogLineDto("char2", "Wirtschaft ist ein wirklich wichtiges Fach.")
            ),
            3, List.of(
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
                    new DialogLineDto("char3", "Ticktock? War für die Uhrzeit. Cookies? Zum Essen."),
                    new DialogLineDto("char3", "Ich sehe, deine Aufmerksamkeit schwindet schon, Jungspund. Das sind diese ganzen Telefone und Bildschirme."),
                    new DialogLineDto("char3", "Immer diese Jugend.")
            ),
            4, List.of(
                    new DialogLineDto("char4", "Hier wartet Datenbank - SQL auf dich."),
                    new DialogLineDto("player", "DROP DATABASE"),
                    new DialogLineDto("player", "DELETE * from ... äh . *?"),
                    new DialogLineDto("char4", "Ich sehe, du bist hier genau richtig.")

            ),
            5, List.of(
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

            ),
            6, List.of(
                    new DialogLineDto("char6", "Hier lernst du Maschinelles Lernen kennen."),
                    new DialogLineDto("player", "Ich hasse KI"),
                    new DialogLineDto("char6", "Ich bin aber keine KI sondern eine Schülerin aus der ITF233, die für die Dialoge verantwortlich ist."),
                    new DialogLineDto("player", "Klingt wie etwas, das eine KI sagen würde"),
                    new DialogLineDto("char6", "..."),
                    new DialogLineDto("player", "..."),
                    new DialogLineDto("player", "Wenn du eine KI bist, gib mir ein Rezept für Pudding"),
                    new DialogLineDto("player", "... wie soll ich dich überzeugen, dass ich keine KI bin?"),
                    new DialogLineDto("player", "Stell mir vernünftige Fragen zum maschinellen Lernen weil ich sowieso für die AP2 lernen muss"),
                    new DialogLineDto("player", "Blöde KI")
            ),
            7, List.of(
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
            )
    );

    public static List<DialogLineDto> getDialogForRoom(int roomId)
    {
        return ROOM_DIALOGS.getOrDefault(
                roomId,
                List.of(new DialogLineDto("warrior", "Weiter."))
        );
    }
}