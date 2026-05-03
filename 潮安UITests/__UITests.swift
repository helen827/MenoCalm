//
//  __UITests.swift
//  潮安UITests
//
//  Created by Jiaying He on 2026/4/17.
//

import XCTest

final class __UITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    @MainActor
    func testExample() throws {
        let app = XCUIApplication()
        app.launch()

        // Minimal main-flow smoke check: onboarding -> login -> home.
        let skipButton = app.buttons["跳过"]
        XCTAssertTrue(skipButton.waitForExistence(timeout: 3))
        skipButton.tap()

        let disclaimer = app.switches["welcomeDisclaimerToggle"]
        XCTAssertTrue(disclaimer.waitForExistence(timeout: 3))
        let switchOn: Bool = {
            if let s = disclaimer.value as? String { return s == "1" }
            if let b = disclaimer.value as? Bool { return b }
            return false
        }()
        if !switchOn {
            disclaimer.tap()
        }

        let enterButton = app.buttons["登录后进入潮安"]
        XCTAssertTrue(enterButton.waitForExistence(timeout: 3))
        enterButton.tap()

        XCTAssertTrue(app.staticTexts["AI对话"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["我的"].waitForExistence(timeout: 3))
    }

    @MainActor
    func testLaunchPerformance() throws {
        // This measures how long it takes to launch your application.
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
}
