import XCTest

final class EditorSmokeTests: XCTestCase {
    func testWelcomeOpensDemoAndBlocksAnonymousGeneration() {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        let demo = app.buttons["Open demo"]
        XCTAssertTrue(demo.waitForExistence(timeout: 20))
        demo.tap()
        let generation = app.buttons["Start generation"]
        XCTAssertTrue(generation.waitForExistence(timeout: 15))
        generation.tap()
        XCTAssertTrue(app.staticTexts["Sign in to Comixon to start generation."].waitForExistence(timeout: 5))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "iOS editor"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
