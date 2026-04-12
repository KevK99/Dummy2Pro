jest.setTimeout(10000);

afterEach(() => {
    // Jede Testausführung bekommt ihre Timer-/Mock-Umgebung sauber zurück.
    // Zusätzlich werden alle registrierten jsdom-Fenster explizit geschlossen,
    // damit keine DOM-Reste zwischen Tests hängenbleiben.
    jest.useRealTimers();
    jest.restoreAllMocks();

    if (global.__dummy2proDomRegistry) {
        for (const dom of global.__dummy2proDomRegistry) {
            try {
                dom.window.close();
            } catch (error) {
                // absichtlich ignorieren
            }
        }

        global.__dummy2proDomRegistry.clear();
    }
});