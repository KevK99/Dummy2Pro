jest.setTimeout(10000);

afterEach(() => {
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