package me.daskabel.dummy2pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import me.daskabel.dummy2pro.dto.RoomDtos.DialogLineDto;
import org.junit.jupiter.api.Test;

/**
 * Unittests für die statischen Raum-Einleitungsdialoge.
 *
 * Geprüft wird, ob bekannte Räume ihren konfigurierten Dialog liefern und
 * unbekannte Räume auf eine definierte Fallback-Zeile zurückfallen.
 */
class RoomIntroDialogsUnitTest
{
    @Test
    void getDialogForKnownRoom_returnsConfiguredDialog()
    {
        List<DialogLineDto> dialog = RoomIntroDialogs.getDialogForRoom(1);

        assertTrue(dialog.size() > 1);
        assertEquals("player", dialog.get(0).getSpeaker());
    }

    @Test
    void getDialogForUnknownRoom_returnsFallbackLine()
    {
        List<DialogLineDto> dialog = RoomIntroDialogs.getDialogForRoom(999);

        assertEquals(1, dialog.size());
        assertEquals("warrior", dialog.get(0).getSpeaker());
        assertEquals("Weiter.", dialog.get(0).getText());
    }
}
