@byok
Feature: Academy BYOK LLM multi-provider (ACADEMY-7)

  The academy workspace resolves the learner's LLM provider from the N0
  contract (LlmProviderKind): the opencode provider id, the AI-SDK package
  and the endpoint all come from the verified catalog, and a key is only
  ever referenced by the NAME of its environment variable (the official
  {env:NAME} interpolation) — never a value.

  Scenario: the default provider is the embedded ollama runtime without a key
    Given a byok config for provider "OLLAMA_LOCAL"
    And a byok model "gpt-oss:120b-cloud"
    And a byok provider url "http://ollama:11434/v1"
    When the byok config generator renders the config
    Then the byok json contains "\"model\": \"ollama/gpt-oss:120b-cloud\""
    And the byok json contains "\"baseURL\": \"http://ollama:11434/v1\""
    And the byok json contains no credential
    And the byok json never references an api key

  Scenario: ollama cloud renders the official endpoint and references its key by name
    Given a byok config for provider "OLLAMA_CLOUD"
    And a byok model "gpt-oss:120b-cloud"
    And a byok provider url "http://ollama:11434/v1"
    When the byok config generator renders the config
    Then the byok json contains "\"model\": \"ollama-cloud/gpt-oss:120b-cloud\""
    And the byok json contains "\"baseURL\": \"https://ollama.com/v1\""
    And the byok json contains "\"apiKey\": \"{env:OLLAMA_API_KEY}\""

  Scenario: gemini uses the official google package and GEMINI_API_KEY
    Given a byok config for provider "GEMINI"
    And a byok model "gemini-2.5-flash"
    And a byok provider url "http://ollama:11434/v1"
    When the byok config generator renders the config
    Then the byok json contains "\"model\": \"google/gemini-2.5-flash\""
    And the byok json contains "\"@ai-sdk/google\""
    And the byok json contains "\"apiKey\": \"{env:GEMINI_API_KEY}\""
    And the byok json does not contain "baseURL"

  Scenario: huggingface points at the router endpoint and HF_TOKEN
    Given a byok config for provider "HUGGINGFACE"
    And a byok model "meta-llama/Llama-3.3-70B-Instruct"
    And a byok provider url "http://ollama:11434/v1"
    When the byok config generator renders the config
    Then the byok json contains "\"baseURL\": \"https://router.huggingface.co/v1\""
    And the byok json contains "\"apiKey\": \"{env:HF_TOKEN}\""

  Scenario: a custom provider carries the learner base url and key variable name
    Given a byok config for provider "CUSTOM"
    And a byok model "gpt-oss:120b-cloud"
    And a byok provider url "https://api.myprovider.com/v1"
    And a byok api key env var "MY_PROVIDER_KEY"
    When the byok config generator renders the config
    Then the byok json contains "\"model\": \"custom/gpt-oss:120b-cloud\""
    And the byok json contains "\"baseURL\": \"https://api.myprovider.com/v1\""
    And the byok json contains "\"apiKey\": \"{env:MY_PROVIDER_KEY}\""

  Scenario: every provider renders balanced json that never embeds a key value
    Given a byok config for provider "OLLAMA_CLOUD"
    And a byok model "gpt-oss:120b-cloud"
    And a byok provider url "http://ollama:11434/v1"
    When the byok config generator renders the config
    Then the byok json braces balance

  Scenario: an external provider tells the learner which variable to export
    Given a byok config for provider "GEMINI"
    And a byok model "gemini-2.5-flash"
    And a byok provider url "http://ollama:11434/v1"
    When the byok learner guide generator renders the guide
    Then the byok guide contains "GEMINI_API_KEY"
    And the byok guide contains "google"

  Scenario: the embedded runtime tells the learner no key is required
    Given a byok config for provider "OLLAMA_LOCAL"
    And a byok model "gpt-oss:120b-cloud"
    And a byok provider url "http://ollama:11434/v1"
    When the byok learner guide generator renders the guide
    Then the byok guide contains "no key"
