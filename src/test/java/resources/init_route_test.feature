Feature: init route test

  Scenario: payment mode +ve validation route test
    Given I can load camel context
    When I call the payment-mode-validation route with "GSMA" payment mode
    Then I should get a non null exchange variable
    And "isPaymentModeValid" exchange variable should be "true"

  Scenario: payment mode -ve validation route test
    Given I can load camel context
    When I call the payment-mode-validation route with "P2P" payment mode
    Then I should get a non null exchange variable
    And "isPaymentModeValid" exchange variable should be "false"

  Scenario: runtime payload test with GSMA mode
    Given I can load camel context
    When I call the runtime-payload route with "GSMA" payment mode
    Then I should get a non null exchange variable
    And The body should be of GSMA parcelable

  Scenario: runtime payload test with gsma mode
    Given I can load camel context
    When I call the runtime-payload route with "gsma" payment mode
    Then I should get a non null exchange variable
    And The body should be of GSMA parcelable

  Scenario: runtime payload test with MOJALOOP mode
    Given I can load camel context
    When I call the runtime-payload route with "MOJALOOP" payment mode
    Then I should get a non null exchange variable
    And The body should be of MOJALOOP parcelable
