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
                    new DialogLineDto("char2", "Willkommen im Raum Wirtschaft.")
            ),
            3, List.of(
                    new DialogLineDto("char3", "Nun betreten wir den Raum Datenbanken Modellierung.")
            ),
            4, List.of(
                    new DialogLineDto("char4", "Hier wartet Datenbank - SQL auf dich.")
            ),
            5, List.of(
                    new DialogLineDto("char5", "Willkommen im Raum UML.")
            ),
            6, List.of(
                    new DialogLineDto("char6", "Hier lernst du Maschinelles Lernen kennen.")
            ),
            7, List.of(
                    new DialogLineDto("char7", "Die letzte Halle: Programmierung Pseudocode.")
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