module.exports = {
    rootDir: ".",
    testEnvironment: "node",
    testMatch: ["<rootDir>/frontend-tests/**/*.test.js"],
    setupFilesAfterEnv: ["<rootDir>/frontend-tests/setup/jest.setup.js"],
    coverageDirectory: "<rootDir>/target/frontend-coverage",
    coverageReporters: ["text", "html", "lcov"],
    coverageProvider: "babel",
    coveragePathIgnorePatterns: [
        "/node_modules/",
        "/frontend-tests/helpers/",
        "/frontend-tests/setup/"
    ],
    clearMocks: true,
    restoreMocks: true,
    verbose: true
};